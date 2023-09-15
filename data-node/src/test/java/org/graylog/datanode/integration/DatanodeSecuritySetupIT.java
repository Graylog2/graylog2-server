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
package org.graylog.datanode.integration;

import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.NoHttpResponseException;
import org.graylog.datanode.configuration.variants.KeystoreInformation;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog2.plugin.Tools;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.shared.utilities.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;

public class DatanodeSecuritySetupIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeSecuritySetupIT.class);

    @TempDir
    static Path tempDir;
    private DatanodeContainerizedBackend backend;
    private String httpUsername;
    private String httpPassword;
    private KeyStore trustStore;
    private String containerHostname;

    @BeforeEach
    void setUp() throws IOException, GeneralSecurityException {

        containerHostname = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        // first generate a self-signed CA
        KeystoreInformation ca = DatanodeSecurityTestUtils.generateCa(tempDir);
        trustStore = DatanodeSecurityTestUtils.buildTruststore(ca);

        // use the CA to generate transport certificate keystore
        final KeystoreInformation transportCert = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, containerHostname);
        // use the CA to generate HTTP certificate keystore
        final KeystoreInformation httpCert = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, containerHostname, Tools.getLocalCanonicalHostname());

        httpUsername = RandomStringUtils.randomAlphabetic(10);
        httpPassword = RandomStringUtils.randomAlphabetic(10);

        backend = new DatanodeContainerizedBackend(containerHostname, datanodeContainer -> {
            // provide the keystore files to the docker container
            datanodeContainer.withFileSystemBind(transportCert.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-transport-certificates.p12");
            datanodeContainer.withFileSystemBind(httpCert.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-https-certificates.p12");

            // configure transport security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", transportCert.passwordAsString());
            datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", DatanodeContainerizedBackend.SIGNING_SECRET);

            // configure http security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", httpCert.passwordAsString());

            // configure initial admin username and password for Opensearch REST
            datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", httpUsername);
            datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", httpPassword);

            // this is the interface that we bind opensearch to. It must be 0.0.0.0 if we want
            // to be able to reach opensearch from outside the container and docker network (true?)
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

            // HOSTNAME is used to generate the SSL certificates and to communicate inside the
            // container and docker network, where we do the hostname validation.
            datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(containerHostname));
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", containerHostname);

            datanodeContainer.withEnv("GRAYLOG_DATANODE_SINGLE_NODE_ONLY", "true");
        }).start();
    }

    @AfterEach
    void tearDown() {
        backend.stop();
    }

    @Test
    void testSecuredSetup() throws ExecutionException, RetryException {
        final String jwtAuthToken = IndexerJwtAuthTokenProvider.createToken(DatanodeContainerizedBackend.SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), Duration.seconds(120));

        waitForOpensearchAvailableStatus(backend.getDatanodeRestPort(), backend.getOpensearchRestPort(), trustStore);

        try {
            given().header( "Authorization", "Bearer " + jwtAuthToken)
                    .trustStore(trustStore)
                    .get("https://localhost:" + backend.getOpensearchRestPort())
                    .then().assertThat()
                    .body("name", Matchers.equalTo(containerHostname))
                    .body("cluster_name", Matchers.equalTo("datanode-cluster"));
        } catch (Exception ex) {
            LOG.error("Error connecting to OpenSearch in the DataNode, showing logs:\n{}", backend.getLogs());
            throw ex;
        }
    }

    private void waitForOpensearchAvailableStatus(final Integer datanodeRestPort, final Integer opensearchPort, final KeyStore trustStore) throws ExecutionException, RetryException {
        final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                .retryIfException(input -> input instanceof NoHttpResponseException)
                .retryIfException(input -> input instanceof SocketException)
                .retryIfException(input -> input instanceof SSLHandshakeException) // may happen before SSL is configured properly
                .retryIfResult(input -> !input.extract().contentType().startsWith("application/json"))
                .retryIfResult(input -> !input.extract().body().path("opensearch.node.state").equals("AVAILABLE"))
                .build();

        try {
            var hostname = Tools.getLocalCanonicalHostname();
            var rest = StringUtils.f("https://%s:%d", hostname, datanodeRestPort);
            var os = StringUtils.f("https://%s:%d", hostname, opensearchPort);
            LOG.info("Trying to connect to REST API at: {}, OpenSearch is at: {}", rest, os);
            retryer.call(() -> RestAssured.given()
                    .auth().basic(httpUsername, httpPassword)
                    .trustStore(trustStore)
                    .get(rest)
                    .then());
        } catch (Exception ex) {
            LOG.error("Error starting the DataNode, showing logs:\n" + backend.getLogs());
            throw ex;
        }
    }
}
