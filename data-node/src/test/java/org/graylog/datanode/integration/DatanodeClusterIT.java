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
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.CertutilCert;
import org.graylog.security.certutil.CertutilHttp;
import org.graylog.security.certutil.console.TestableConsole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;

public class DatanodeClusterIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeClusterIT.class);

    private DatanodeContainerizedBackend nodeA;
    private DatanodeContainerizedBackend nodeB;

    @TempDir
    static Path tempDir;
    private String hostnameNodeA;
    private KeyStore trustStore;
    private String usernameNodeA;
    private String passwordNodeA;

    @BeforeEach
    void setUp() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        // first generate a self-signed CA
        final KeystoreInformation ca = generateCa();

        trustStore = buildTruststore(ca);

        usernameNodeA = RandomStringUtils.randomAlphabetic(10);
        passwordNodeA = RandomStringUtils.randomAlphabetic(10);

        final String usernameNodeB = RandomStringUtils.randomAlphabetic(10);
        final String passwordNodeB = RandomStringUtils.randomAlphabetic(10);

        hostnameNodeA = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final KeystoreInformation transportNodeA = generateNodeCert(ca, "nodeA");
        final KeystoreInformation httpNodeA = generateHttpCert(ca, "nodeA");

        final String hostnameNodeB = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final KeystoreInformation transportNodeB = generateNodeCert(ca, "nodeB");
        final KeystoreInformation httpNodeB = generateHttpCert(ca, "nodeB");

        nodeA = new DatanodeContainerizedBackend(hostnameNodeA, datanodeContainer -> {
            datanodeContainer.withEnv("GRAYLOG_DATANODE_CLUSTER_INITIAL_MANAGER_NODES", hostnameNodeA);
            datanodeContainer.withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", hostnameNodeA + ":9300");

            datanodeContainer.withFileSystemBind(transportNodeA.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-transport-certificates.p12");
            datanodeContainer.withFileSystemBind(httpNodeA.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-https-certificates.p12");

            // configure transport security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", transportNodeA.passwordAsString());
            datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");

            // configure http security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", httpNodeA.passwordAsString());

            // configure initial admin username and password for Opensearch REST
            datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", usernameNodeA);
            datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", passwordNodeA);

            // this is the interface that we bind opensearch to. It must be 0.0.0.0 if we want
            // to be able to reach opensearch from outside the container and docker network (true?)
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

            // HOSTNAME is used to generate the SSL certificates and to communicate inside the
            // container and docker network, where we do the hostname validation.
            datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(hostnameNodeA));
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", hostnameNodeA);
        }).start();

        nodeB = new DatanodeContainerizedBackend(
                nodeA.getNetwork(),
                nodeA.getMongodbContainer(),
                hostnameNodeB,
                datanodeContainer -> {
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_CLUSTER_INITIAL_MANAGER_NODES", hostnameNodeA);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", hostnameNodeA + ":9300");

                    datanodeContainer.withFileSystemBind(transportNodeB.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-transport-certificates.p12");
                    datanodeContainer.withFileSystemBind(httpNodeB.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-https-certificates.p12");

                    // configure transport security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", transportNodeB.passwordAsString());
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");

                    // configure http security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", httpNodeB.passwordAsString());

                    // configure initial admin username and password for Opensearch REST
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", usernameNodeB);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", passwordNodeB);

                    // this is the interface that we bind opensearch to. It must be 0.0.0.0 if we want
                    // to be able to reach opensearch from outside the container and docker network (true?)
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

                    // HOSTNAME is used to generate the SSL certificates and to communicate inside the
                    // container and docker network, where we do the hostname validation.
                    datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(hostnameNodeB));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", hostnameNodeB);
                }
        );
        nodeB.start();
    }

    private KeyStore buildTruststore(KeystoreInformation ca) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        try (FileInputStream fis = new FileInputStream(ca.location().toFile())) {

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fis, ca.password());

            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);

            final Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                final Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof final X509Certificate x509Certificate) {
                    trustStore.setCertificateEntry(alias, x509Certificate);
                }
            }
            return keyStore;
        }
    }

    @AfterEach
    void tearDown() {
        nodeB.stop();
        nodeA.stop();
    }

    @Test
    void testClusterFormation() throws ExecutionException, RetryException {

        try {
            final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                    .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                    .retryIfException(input -> input instanceof NoHttpResponseException)
                    .retryIfException(input -> input instanceof SocketException)
                    .retryIfResult(input -> !input.extract().body().path("number_of_nodes").equals(2))
                    .retryIfResult(input -> !input.extract().body().path("status").equals("green"))
                    .build();

            final Integer opensearchPort = nodeA.getOpensearchRestPort();

            retryer.call(() -> this.getStatus(opensearchPort))
                    .assertThat()
                    .body("status", Matchers.equalTo("green"))
                    .body("number_of_nodes", Matchers.equalTo(2))
                    .body("discovered_cluster_manager", Matchers.equalTo(true));
        } catch (RetryException retryException) {
            LOG.error("DataNode Container logs follow:\n" + nodeA.getLogs());
            throw retryException;
        }
    }

    private ValidatableResponse getStatus(Integer mappedPort) {
        return RestAssured.given()
                .trustStore(trustStore)
                .auth().basic(usernameNodeA, passwordNodeA)
                .get("https://localhost:" + mappedPort + "/_cluster/health")
                .then();
    }


    private KeystoreInformation generateCa() {
        final Path certPath = tempDir.resolve("test-ca.p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        final TestableConsole input = TestableConsole.empty().silent()
                .register("Enter CA password", password);
        final CertutilCa command = new CertutilCa(certPath.toAbsolutePath().toString(), input);
        command.run();
        return new KeystoreInformation(certPath, password);
    }

    private KeystoreInformation generateNodeCert(KeystoreInformation ca, String containerHostname) {
        final Path nodePath = tempDir.resolve("transport-" + containerHostname + ".p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        TestableConsole inputCert = TestableConsole.empty().silent()
                .register("Enter CA password", ca.passwordAsString())
                .register("Enter datanode certificate password", password)
                .register("Enter alternative names (addresses) of this node [comma separated]", containerHostname);
        CertutilCert certutilCert = new CertutilCert(
                ca.location().toAbsolutePath().toString(),
                nodePath.toAbsolutePath().toString(),
                inputCert);
        certutilCert.run();
        return new KeystoreInformation(nodePath, password);
    }

    private KeystoreInformation generateHttpCert(KeystoreInformation ca, String containerHostname) {
        final Path httpPath = tempDir.resolve("http-" + containerHostname + ".p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        TestableConsole inputHttp = TestableConsole.empty().silent()
                .register("Do you want to use your own certificate authority? Respond with y/n?", "n")
                .register("Enter CA password", ca.passwordAsString())
                .register("Enter certificate validity in days", "90")
                .register("Enter alternative names (addresses) of this node [comma separated]", containerHostname)
                .register("Enter HTTP certificate password", password);
        CertutilHttp certutilCert = new CertutilHttp(
                ca.location().toAbsolutePath().toString(),
                httpPath.toAbsolutePath().toString(),
                inputHttp);
        certutilCert.run();
        return new KeystoreInformation(httpPath, password);
    }
}
