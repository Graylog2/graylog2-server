/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.console.TestableConsole;
import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.restoperations.DatanodeOpensearchWait;
import org.graylog.testing.restoperations.RestOperationParameters;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.security.JwtSecret;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV,
                                   additionalConfigurationParameters = {
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"),
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = ""),
                                   })
public class DatanodeProvisioningIT {

    private final Logger log = LoggerFactory.getLogger(DatanodeProvisioningIT.class);

    private final GraylogApis apis;

    @TempDir
    private Path tempDir;
    private BasicAuthCredentials basicAuth;

    public DatanodeProvisioningIT(GraylogApis apis) {
        this.apis = apis;
    }

    @BeforeEach
    void setUp() {
        basicAuth = extractBasicAuthFromLogs(apis.backend().getLogs());
    }

    @AfterEach
    void tearDown() {
        resetPreflight();
    }

    @ContainerMatrixTest
    void provisionDatanodeGenerateCA() throws ExecutionException, RetryException, KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        final String caSubjectName = createSelfSignedCA();

        configureAutomaticCertRenewalPolicy();
        triggerDatanodeProvisioning();
        // Wait till all the datanodes become CONNECTED (=stated with certificates)
        final List<DatanodeStatus> connectedDatanodes = waitForDatanodesConnected(basicAuth);
        // verify that we have one connected datanode
        Assertions.assertThat(connectedDatanodes)
                .hasSize(1);

        final KeyStore truststore = keystoreFromApiCertificate();
        verifySubjectName(truststore, caSubjectName);

        testEncryptedConnectionToOpensearch(truststore);
    }

    private static void verifySubjectName(KeyStore truststore, String caSubjectName) throws KeyStoreException {
        final X500Principal subject = ((X509Certificate) truststore.getCertificate("ca")).getSubjectX500Principal();
        Assertions.assertThat(subject.getName()).isEqualTo("CN=" + caSubjectName);
    }

    private void testEncryptedConnectionToOpensearch(KeyStore truststore) throws ExecutionException, RetryException, KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        try {
            new DatanodeOpensearchWait(RestOperationParameters.builder()
                    .port(getOpensearchPort())
                    .truststore(truststore)
                    .jwtTokenProvider(new IndexerJwtAuthTokenProvider(new JwtSecret(ContainerizedGraylogBackend.PASSWORD_SECRET), Duration.seconds(120), Duration.seconds(60)))
                    .build())
                    .waitForNodesCount(1);
        } catch (Exception e) {
            log.error("Could not connect to Opensearch\n" + apis.backend().getSearchLogs());
            throw e;
        }
    }

    private List<DatanodeStatus> waitForDatanodesConnected(BasicAuthCredentials basicAuth) throws ExecutionException, RetryException {
        List<DatanodeStatus> connectedDatanodes = null;
        try {
            connectedDatanodes = RetryerBuilder.<List<DatanodeStatus>>newBuilder()
                    .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.stopAfterAttempt(60))
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            if (attempt.hasResult()) {
                                log.info(String.valueOf(attempt.getResult()));
                            }
                        }
                    })
                    .retryIfResult(list -> list.isEmpty() || !list.stream().allMatch(node ->
                            node.status().equals(DataNodeProvisioningConfig.State.CONNECTED.name()) &&
                                    node.dataNodeStatus().equals(DataNodeStatus.AVAILABLE.name())
                    ))
                    .build()
                    .call(this::getDatanodes);
        } catch (ExecutionException | RetryException | IllegalStateException e) {
            log.error("Datanode not started:\n" + apis.backend().getSearchLogs());
            throw e;
        }
        return connectedDatanodes;
    }

    private ValidatableResponse triggerDatanodeProvisioning() {
        return given()
                .spec(apis.requestSpecification())
                .body("")
                .auth().basic(basicAuth.username, basicAuth.password)
                .post("/generate")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    private String createSelfSignedCA() {
        String subject = "Graylog CA generated " + RandomStringUtils.randomAlphanumeric(10);
        given()
                .spec(apis.requestSpecification())
                .body("{\"organization\":\"" + subject + "\"}")
                .auth().basic(basicAuth.username, basicAuth.password)
                .post("/ca/create")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
        return subject;
    }

    private void resetPreflight() {
        given()
                .spec(apis.requestSpecification())
                .auth().basic(basicAuth.username, basicAuth.password)
                .delete("/startOver")
                .then()
                .log().ifStatusCodeMatches(Matchers.not(not(HttpStatus.SC_NO_CONTENT)))
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    private ValidatableResponse configureAutomaticCertRenewalPolicy() {
        return given()
                .spec(apis.requestSpecification())
                .body("{\"mode\":\"Automatic\",\"certificate_lifetime\":\"P30D\"}")
                .auth().basic(basicAuth.username, basicAuth.password)
                .post("/renewal_policy")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @ContainerMatrixTest
    void provisionDatanodeUploadCA() throws ExecutionException, RetryException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        final Path caKeystore = createCA();

        uploadCA(caKeystore);
        configureAutomaticCertRenewalPolicy();
        triggerDatanodeProvisioning();
        // Wait till all the datanodes become CONNECTED (=stated with certificates)
        final List<DatanodeStatus> connectedDatanodes = waitForDatanodesConnected(basicAuth);
        // verify that we have one connected datanode
        Assertions.assertThat(connectedDatanodes)
                .hasSize(1);

        final KeyStore truststore = keystoreFromApiCertificate();
        verifySubjectName(truststore, CertutilCa.DEFAULT_ORGANIZATION_NAME);

        testEncryptedConnectionToOpensearch(truststore);
    }

    private ValidatableResponse uploadCA(Path caKeystore) {
        return given()
                .spec(apis.requestSpecification())
                .auth().basic(basicAuth.username, basicAuth.password)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPart("files", caKeystore.toFile())
                .multiPart("password", "my-secret-password")
                .post("/ca/upload")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    private Path createCA() {
        final Path certPath = tempDir.resolve("test-ca.p12");

        final TestableConsole input = TestableConsole.empty()
                .register(CertutilCa.PROMPT_ENTER_CA_PASSWORD, "my-secret-password");

        final CertutilCa command = new CertutilCa(certPath.toAbsolutePath().toString(), input);
        command.run();
        return certPath;
    }

    @Nonnull
    private KeyStore keystoreFromApiCertificate() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        final byte[] certificate = given()
                .spec(apis.requestSpecification())
                .auth().basic(basicAuth.username, basicAuth.password)
                .accept(ContentType.TEXT)
                .get("/ca/certificate")
                .then().extract().body().asByteArray();
        final KeyStore keystore = KeyStore.getInstance(CertConstants.PKCS12);
        keystore.load(null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certificate));
        keystore.setCertificateEntry("ca", cert);
        return keystore;
    }

    private int getOpensearchPort() {
        final String indexerHostAddress = apis.backend().searchServerInstance().getHttpHostAddress();
        return Integer.parseInt(indexerHostAddress.split(":")[1]);
    }

    private List<DatanodeStatus> getDatanodes() {
        return given()
                .spec(apis.requestSpecification())
                .auth().basic(basicAuth.username, basicAuth.password)
                .get("/data_nodes")
                .then()
                .extract().body().as(new TypeRef<List<DatanodeStatus>>() {});

    }

    private BasicAuthCredentials extractBasicAuthFromLogs(String logs) {
        final Pattern pattern = Pattern.compile("Initial configuration is accessible at .+ with username '(.+)' and password '(.+)'", Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(logs);
        if (matcher.find()) {
            return new BasicAuthCredentials(matcher.group(1), matcher.group(2));
        } else {
            throw new IllegalStateException("Couldn't find preflight auth credentials in logs: " + logs);
        }
    }

    private record BasicAuthCredentials(String username, String password) {}

    private record DatanodeStatus(
            @JsonProperty("node_id") String nodeId,
            @JsonProperty("transport_address") String transportAddress,
            @JsonProperty("status") String status,
            @JsonProperty("error_msg") String errorMsg,
            @JsonProperty("hostname") String hostname,
            @JsonProperty("short_node_id") String shortNodeId,
            @JsonProperty("data_node_status") String dataNodeStatus
    ) {
    }
}
