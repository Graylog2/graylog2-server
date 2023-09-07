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
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.DatanodeOpensearchWait;
import org.graylog.datanode.configuration.variants.KeystoreInformation;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;

public class DatanodeClusterIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeClusterIT.class);

    private DatanodeContainerizedBackend nodeA;
    private DatanodeContainerizedBackend nodeB;

    @TempDir
    static Path tempDir;
    private String hostnameNodeA;
    private KeyStore trustStore;
    private String initialAdminUsername;
    private String initialAdminPassword;
    private KeystoreInformation ca;
    private Network network;
    private GenericContainer<?> mongodbContainer;

    @BeforeEach
    void setUp() throws GeneralSecurityException, IOException {

        // first generate a self-signed CA
        ca = DatanodeSecurityTestUtils.generateCa(tempDir);

        trustStore = DatanodeSecurityTestUtils.buildTruststore(ca);

        hostnameNodeA = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final KeystoreInformation transportNodeA = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeA);
        final KeystoreInformation httpNodeA = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeA);
        initialAdminUsername = RandomStringUtils.randomAlphabetic(10);
        initialAdminPassword = RandomStringUtils.randomAlphabetic(10);

        this.network = Network.newNetwork();
        this.mongodbContainer = DatanodeContainerizedBackend.createMongodbContainer(network);
        this.mongodbContainer.start();

        nodeA = createDatanodeContainer(
                network,
                mongodbContainer,
                hostnameNodeA,
                transportNodeA,
                httpNodeA,
                initialAdminUsername,
                initialAdminPassword
        );


        final String hostnameNodeB = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final KeystoreInformation transportNodeB = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeB);
        final KeystoreInformation httpNodeB = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeB);

        nodeB = createDatanodeContainer(
                network,
                mongodbContainer,
                hostnameNodeB,
                transportNodeB,
                httpNodeB,
                initialAdminUsername,
                initialAdminPassword
        );

        Stream.of(nodeA, nodeB).parallel().forEach(DatanodeContainerizedBackend::start);
    }

    @AfterEach
    void tearDown() {
        nodeB.stop();
        nodeA.stop();
        mongodbContainer.stop();
        network.close();
    }

    @Test
    void testClusterFormation() throws ExecutionException, RetryException {
        waitForNodesCount(2);
    }

    @Test
    void testAddingNodeToExistingCluster() throws ExecutionException, RetryException {

        final String hostnameNodeC = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final KeystoreInformation transportNodeC = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeC);
        final KeystoreInformation httpNodeC = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeC);

        final DatanodeContainerizedBackend nodeC = createDatanodeContainer(
                nodeA.getNetwork(), nodeA.getMongodbContainer(),
                hostnameNodeC,
                transportNodeC,
                httpNodeC,
                initialAdminUsername,
                initialAdminPassword
        );

        nodeC.start();
        waitForNodesCount(3);
        nodeC.stop();
        waitForNodesCount(2);
    }

    @NotNull
    private DatanodeContainerizedBackend createDatanodeContainer(Network network, GenericContainer<?> mongodb, String hostname, KeystoreInformation transportKeystore, KeystoreInformation httpKeystore, String restUsername, String restPassword) {
        return new DatanodeContainerizedBackend(
                network,
                mongodb,
                hostname,
                datanodeContainer -> {
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_CLUSTER_INITIAL_MANAGER_NODES", hostnameNodeA);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", hostnameNodeA + ":9300");

                    datanodeContainer.withFileSystemBind(transportKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-transport-certificates.p12");
                    datanodeContainer.withFileSystemBind(httpKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/bin/config/datanode-https-certificates.p12");

                    // configure transport security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", transportKeystore.passwordAsString());
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");

                    // configure http security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", httpKeystore.passwordAsString());

                    // configure initial admin username and password for Opensearch REST
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", restUsername);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", restPassword);

                    // this is the interface that we bind opensearch to. It must be 0.0.0.0 if we want
                    // to be able to reach opensearch from outside the container and docker network (true?)
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

                    // HOSTNAME is used to generate the SSL certificates and to communicate inside the
                    // container and docker network, where we do the hostname validation.
                    datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(hostname));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", hostname);
                });
    }

    private void waitForNodesCount(int countOfNodes) throws ExecutionException, RetryException {
        try {
            DatanodeOpensearchWait.onPort(nodeA.getOpensearchRestPort())
                    .withTruststore(trustStore)
                    .withBasicAuth(initialAdminUsername, initialAdminPassword)
                    .waitForNodesCount(countOfNodes);
        } catch (Exception retryException) {
            LOG.error("DataNode Container logs from nodeA follow:\n" + nodeA.getLogs());
            LOG.error("DataNode Container logs from node B follow:\n" + nodeB.getLogs());
            throw retryException;
        }
    }
}
