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
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.restoperations.DatanodeOpensearchWait;
import org.graylog.testing.restoperations.DatanodeRestApiWait;
import org.graylog.testing.restoperations.DatanodeStatusChangeOperation;
import org.graylog.testing.restoperations.OpensearchTestIndexCreation;
import org.graylog.testing.restoperations.RestOperationParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatanodeClusterIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeClusterIT.class);

    private DatanodeContainerizedBackend nodeA;
    private DatanodeContainerizedBackend nodeB;
    private DatanodeContainerizedBackend nodeC;

    @TempDir
    static Path tempDir;
    private String hostnameNodeA;
    private KeyStore trustStore;
    private FilesystemKeystoreInformation ca;
    private Network network;
    private MongoDBTestService mongoDBTestService;

    @BeforeEach
    void setUp() throws GeneralSecurityException, IOException {
        // first generate a self-signed CA
        ca = DatanodeSecurityTestUtils.generateCa(tempDir);

        trustStore = DatanodeSecurityTestUtils.buildTruststore(ca);

        hostnameNodeA = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final FilesystemKeystoreInformation transportNodeA = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeA);
        final FilesystemKeystoreInformation httpNodeA = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeA);

        this.network = Network.newNetwork();
        this.mongoDBTestService = MongoDBTestService.create(MongodbServer.DEFAULT_VERSION, network);
        this.mongoDBTestService.start();

        nodeA = createDatanodeContainer(
                network,
                mongoDBTestService,
                hostnameNodeA,
                transportNodeA,
                httpNodeA
        );


        final String hostnameNodeB = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final FilesystemKeystoreInformation transportNodeB = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeB);
        final FilesystemKeystoreInformation httpNodeB = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeB);

        nodeB = createDatanodeContainer(
                network,
                mongoDBTestService,
                hostnameNodeB,
                transportNodeB,
                httpNodeB
        );

        Stream.of(nodeA, nodeB).parallel().forEach(DatanodeContainerizedBackend::start);
    }

    @AfterEach
    void tearDown() {
        if (nodeB != null) {
            nodeB.stop();
        }
        if (nodeA != null) {
            nodeA.stop();
        }
        if (nodeC != null) {
            nodeC.stop();
        }
        mongoDBTestService.close();
        network.close();
    }

    @Test
    void testClusterFormation() throws ExecutionException, RetryException {
        waitForNodesCount(2);
    }

    @Test
    void testAddingNodeToExistingCluster() throws ExecutionException, RetryException {

        final String hostnameNodeC = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final FilesystemKeystoreInformation transportNodeC = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeC);
        final FilesystemKeystoreInformation httpNodeC = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeC);

        nodeC = createDatanodeContainer(
                network, mongoDBTestService,
                hostnameNodeC,
                transportNodeC,
                httpNodeC
        );

        nodeC.start();
        waitForNodesCount(3);
        nodeC.stop();
        waitForNodesCount(2);
    }

    @Test
    void testRemovingNodeReallocatesShards() throws ExecutionException, RetryException {

        final String hostnameNodeC = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final FilesystemKeystoreInformation transportNodeC = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeC);
        final FilesystemKeystoreInformation httpNodeC = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeC);

        nodeC = createDatanodeContainer(
                network, mongoDBTestService,
                hostnameNodeC,
                transportNodeC,
                httpNodeC
        );

        nodeC.start();
        waitForNodesCount(3);

        OpensearchTestIndexCreation osIndexClient = new OpensearchTestIndexCreation(RestOperationParameters.builder()
                .port(nodeA.getOpensearchRestPort())
                .truststore(trustStore)
                .jwtTokenProvider(DatanodeContainerizedBackend.JWT_AUTH_TOKEN_PROVIDER)
                .build());

        // create index and get primary and replica shard node
        osIndexClient.createIndex();
        List<String> shardNodes = osIndexClient.getShardNodes();
        Assertions.assertEquals(shardNodes.size(), 2);

        List<DatanodeContainerizedBackend> nodes = List.of(nodeA, nodeB, nodeC);
        final Optional<DatanodeContainerizedBackend> primary = nodes.stream().filter(n -> shardNodes.get(0).equals(n.getNodeName())).findFirst();
        assertTrue(primary.isPresent());
        final Optional<DatanodeContainerizedBackend> replica = nodes.stream().filter(n -> shardNodes.get(1).equals(n.getNodeName())).findFirst();
        assertTrue(replica.isPresent());

        // remove node for primary shard, waiting for it to be in AVAILABLE state first
        final RestOperationParameters datanodeRestParameters = RestOperationParameters.builder()
                .port(primary.get().getDatanodeRestPort())
                .truststore(trustStore)
                .jwtTokenProvider(DatanodeContainerizedBackend.JWT_AUTH_TOKEN_PROVIDER)
                .build();
        new DatanodeRestApiWait(datanodeRestParameters)
                .waitForAvailableStatus();
        new DatanodeStatusChangeOperation(datanodeRestParameters)
                .triggerNodeRemoval();


        // check that primary shard node is gone and there are still a primary and a secondary
        waitForNodesCount(replica.get(), 2);
        osIndexClient = new OpensearchTestIndexCreation(RestOperationParameters.builder()
                .port(replica.get().getOpensearchRestPort())
                .truststore(trustStore)
                .jwtTokenProvider(DatanodeContainerizedBackend.JWT_AUTH_TOKEN_PROVIDER)
                .build());
        List<String> newShardNodes = osIndexClient.getShardNodes();
        Assertions.assertEquals(newShardNodes.size(), 2);
        Assertions.assertFalse(newShardNodes.contains(primary.get().getNodeName()));
    }

    @NotNull
    private DatanodeContainerizedBackend createDatanodeContainer(Network network,
                                                                 MongoDBTestService mongodb,
                                                                 String hostname,
                                                                 FilesystemKeystoreInformation transportKeystore,
                                                                 FilesystemKeystoreInformation httpKeystore) {
        return new DatanodeContainerizedBackend(
                network,
                mongodb,
                hostname,
                datanodeContainer -> {
                    datanodeContainer.withNetwork(network);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", DatanodeContainerizedBackend.SIGNING_SECRET);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_INITIAL_CLUSTER_MANAGER_NODES", hostnameNodeA);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", hostnameNodeA + ":9300");

                    datanodeContainer.withFileSystemBind(transportKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-transport-certificates.p12");
                    datanodeContainer.withFileSystemBind(httpKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-https-certificates.p12");

                    // configure transport security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", new String(transportKeystore.password()));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");

                    // configure http security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", new String(httpKeystore.password()));

                    // this is the interface that we bind opensearch to. It must be 0.0.0.0 if we want
                    // to be able to reach opensearch from outside the container and docker network (true?)
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

                    // HOSTNAME is used to generate the SSL certificates and to communicate inside the
                    // container and docker network, where we do the hostname validation.
                    datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(hostname));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", hostname);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_MONGODB_URI", mongodb.internalUri());
                });
    }


    private void waitForNodesCount(final int countOfNodes) throws ExecutionException, RetryException {
        waitForNodesCount(nodeA, countOfNodes);
    }

    private void waitForNodesCount(DatanodeContainerizedBackend node, final int countOfNodes) throws ExecutionException, RetryException {
        try {
            new DatanodeOpensearchWait(RestOperationParameters.builder()
                    .port(node.getOpensearchRestPort())
                    .truststore(trustStore)
                    .jwtTokenProvider(DatanodeContainerizedBackend.JWT_AUTH_TOKEN_PROVIDER)
                    .build())
                    .waitForNodesCount(countOfNodes);

        } catch (Exception retryException) {
            LOG.error("DataNode Container logs from node A follow:\n" + nodeA.getLogs());
            LOG.error("DataNode Container logs from node B follow:\n" + nodeB.getLogs());
            if (nodeC != null) {
                LOG.error("DataNode Container logs from node C follow:\n" + nodeC.getLogs());
            }
            throw retryException;
        }
    }
}
