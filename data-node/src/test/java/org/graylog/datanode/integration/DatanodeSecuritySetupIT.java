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
import org.apache.http.NoHttpResponseException;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.CertutilCert;
import org.graylog.security.certutil.CertutilHttp;
import org.graylog.security.certutil.console.TestableConsole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;


public class DatanodeSecuritySetupIT {

    @TempDir
    static Path tempDir;
    private DatanodeContainerizedBackend backend;
    private Path httpCert;

    @BeforeEach
    void setUp() {
        // first generate a self-signed CA
        final Path ca = generateCa();

        // use the CA to generate transport certificate keystore
        final Path nodeCert = generateNodeCert(ca);
        // use the CA to generate HTTP certificate keystore
        httpCert = generateHttpCert(ca);

        backend = new DatanodeContainerizedBackend(datanodeContainer -> {
            // provide the keystore files to the docker container
            datanodeContainer.withFileSystemBind(nodeCert.toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-transport-certificates.p12");
            datanodeContainer.withFileSystemBind(httpCert.toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-https-certificates.p12");

            // configure transport security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", "password");

            // configure http security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", "password");

            // configure initial admin username and password for Opensearch REST
            datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", "admin");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", "admin");
        }).start();
    }

    @Test
    void testSecuredSetup() throws ExecutionException, RetryException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {

        waitForOpensearchAvailableStatus(backend.getDatanodeRestPort());

        given()
                .auth().basic("admin", "admin")
                .trustStore(buildTruststore(httpCert, "password"))
                .get("https://localhost:" + backend.getOpensearchRestPort())
                .then().assertThat()
                .body("name", Matchers.equalTo("node1"))
                .body("cluster_name", Matchers.equalTo("datanode-cluster"));

    }

    private void waitForOpensearchAvailableStatus(Integer datanodeRestPort) throws ExecutionException, RetryException {
        final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                .retryIfException(input -> input instanceof NoHttpResponseException)
                .retryIfException(input -> input instanceof SocketException)
                .retryIfResult(input -> !input.extract().body().path("process.info.status").equals("AVAILABLE"))
                .build();

        retryer.call(() -> RestAssured.given()
                .get("http://localhost:" + datanodeRestPort)
                .then());
    }

    /**
     * Take the HTTP keystore and extract CA certificate chain as a truststore.
     */
    private KeyStore buildTruststore(Path keystorePath, String password) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        try (FileInputStream fis = new FileInputStream(keystorePath.toFile())) {

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fis, password.toCharArray());

            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);

            final Certificate[] certs = keyStore.getCertificateChain("datanode");
            for (Certificate cert : certs) {
                if (cert instanceof final X509Certificate x509Certificate) {
                    final String alias = x509Certificate.getSubjectX500Principal().getName();
                    trustStore.setCertificateEntry(alias, x509Certificate);
                }
            }
            return trustStore;
        }
    }

    private Path generateCa() {
        final Path certPath = tempDir.resolve("test-ca.p12");
        final TestableConsole input = TestableConsole.empty().silent()
                .register("Enter CA password", "password");
        final CertutilCa command = new CertutilCa(certPath.toAbsolutePath().toString(), input);
        command.run();
        return certPath;
    }

    private Path generateNodeCert(Path caPath) {
        final Path nodePath = tempDir.resolve("test-node.p12");
        TestableConsole inputCert = TestableConsole.empty().silent()
                .register("Enter CA password", "password")
                .register("Enter datanode certificate password", "password");
        CertutilCert certutilCert = new CertutilCert(
                caPath.toAbsolutePath().toString(),
                nodePath.toAbsolutePath().toString(),
                inputCert);
        certutilCert.run();
        return nodePath;
    }

    private Path generateHttpCert(Path caPath) {
        final Path httpPath = tempDir.resolve("test-http.p12");
        TestableConsole inputHttp = TestableConsole.empty().silent()
                .register("Do you want to use your own certificate authority? Respond with y/n?", "n")
                .register("Enter CA password", "password")
                .register("Enter certificate validity in days", "90")
                .register("Enter alternative names (addresses) of this node [comma separated]", "example.com")
                .register("Enter HTTP certificate password", "password");
        CertutilHttp certutilCert = new CertutilHttp(
                caPath.toAbsolutePath().toString(),
                httpPath.toAbsolutePath().toString(),
                inputHttp);
        certutilCert.run();
        return httpPath;
    }

}
