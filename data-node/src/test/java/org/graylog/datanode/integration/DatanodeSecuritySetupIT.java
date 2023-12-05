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
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.configuration.variants.KeystoreInformation;
import org.graylog.datanode.restoperations.DatanodeRestApiWait;
import org.graylog.datanode.restoperations.RestOperationParameters;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog2.plugin.Tools;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;
import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;
import static org.graylog.testing.completebackend.ContainerizedGraylogBackend.ROOT_PASSWORD_PLAINTEXT;

public class DatanodeSecuritySetupIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeSecuritySetupIT.class);

    @TempDir
    static Path tempDir;
    private DatanodeContainerizedBackend backend;
    private KeyStore trustStore;
    private String containerHostname;
    private String restAdminUsername;

    @BeforeEach
    void setUp() throws IOException, GeneralSecurityException {

        restAdminUsername = RandomStringUtils.randomAlphanumeric(10);

        containerHostname = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        // first generate a self-signed CA
        KeystoreInformation ca = DatanodeSecurityTestUtils.generateCa(tempDir);
        trustStore = DatanodeSecurityTestUtils.buildTruststore(ca);

        // use the CA to generate transport certificate keystore
        final KeystoreInformation transportCert = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, containerHostname);
        // use the CA to generate HTTP certificate keystore
        final KeystoreInformation httpCert = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, containerHostname, Tools.getLocalCanonicalHostname());

        backend = new DatanodeContainerizedBackend(containerHostname, datanodeContainer -> {
            // provide the keystore files to the docker container
            datanodeContainer.withFileSystemBind(transportCert.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-transport-certificates.p12");
            datanodeContainer.withFileSystemBind(httpCert.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-https-certificates.p12");

            // configure transport security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", transportCert.passwordAsString());
            datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");

            // configure http security
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", httpCert.passwordAsString());

            // this is the interface that we bind opensearch to. It must be 0.0.0.0 if we want
            // to be able to reach opensearch from outside the container and docker network (true?)
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

            // HOSTNAME is used to generate the SSL certificates and to communicate inside the
            // container and docker network, where we do the hostname validation.
            datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(containerHostname));
            datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", containerHostname);

            datanodeContainer.withEnv("GRAYLOG_DATANODE_SINGLE_NODE_ONLY", "true");

            datanodeContainer.withEnv("GRAYLOG_DATANODE_ROOT_USERNAME", restAdminUsername);
        }).start();
    }

    @AfterEach
    void tearDown() {
        backend.stop();
    }

    @Test
    void testSecuredSetup() throws ExecutionException, RetryException {
        final String jwtAuthToken = IndexerJwtAuthTokenProvider.createToken(DatanodeContainerizedBackend.SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), Duration.seconds(120));

        waitForOpensearchAvailableStatus(backend.getDatanodeRestPort(), trustStore, restAdminUsername, ROOT_PASSWORD_PLAINTEXT);

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

    private ValidatableResponse waitForOpensearchAvailableStatus(final Integer datanodeRestPort, final KeyStore trustStore, String authUsername, String authPassword) throws ExecutionException, RetryException {

        try {
            return new DatanodeRestApiWait(
                    RestOperationParameters.builder()
                            .port(datanodeRestPort)
                            .truststore(trustStore)
                            .username(authUsername)
                            .password(authPassword)
                            .build())
                    .waitForAvailableStatus();
        } catch (Exception ex) {
            LOG.error("Error starting the DataNode, showing logs:\n" + backend.getLogs());
            throw ex;
        }
    }
}
