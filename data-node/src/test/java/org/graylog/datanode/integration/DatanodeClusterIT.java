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
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.configuration.variants.KeystoreInformation;
import org.graylog.datanode.restoperations.DatanodeOpensearchWait;
import org.graylog.datanode.restoperations.DatanodeRestApiWait;
import org.graylog.datanode.restoperations.DatanodeStatusChangeOperation;
import org.graylog.datanode.restoperations.OpensearchTestIndexCreation;
import org.graylog.datanode.restoperations.RestOperationParameters;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @TempDir
    static Path tempDir;
    private String hostnameNodeA;
    private KeyStore trustStore;
    private KeystoreInformation ca;
    private Network network;
    private MongoDBTestService mongoDBTestService;
    private String restAdminUsername;

    @BeforeEach
    void setUp() throws GeneralSecurityException, IOException {

        restAdminUsername = RandomStringUtils.randomAlphanumeric(10);

        // first generate a self-signed CA
        ca = DatanodeSecurityTestUtils.generateCa(tempDir);

        trustStore = DatanodeSecurityTestUtils.buildTruststore(ca);

        hostnameNodeA = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final KeystoreInformation transportNodeA = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeA);
        final KeystoreInformation httpNodeA = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeA);

        this.network = Network.newNetwork();
        this.mongoDBTestService = MongoDBTestService.create(MongodbServer.MONGO5, network);
        this.mongoDBTestService.start();

        nodeA = createDatanodeContainer(
                network,
                mongoDBTestService,
                hostnameNodeA,
                transportNodeA,
                httpNodeA
        );


        final String hostnameNodeB = "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef");
        final KeystoreInformation transportNodeB = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeB);
        final KeystoreInformation httpNodeB = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeB);

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
        final KeystoreInformation transportNodeC = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeC);
        final KeystoreInformation httpNodeC = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeC);

        final DatanodeContainerizedBackend nodeC = createDatanodeContainer(
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
        final KeystoreInformation transportNodeC = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostnameNodeC);
        final KeystoreInformation httpNodeC = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostnameNodeC);

        final DatanodeContainerizedBackend nodeC = createDatanodeContainer(
                network, mongoDBTestService,
                hostnameNodeC,
                transportNodeC,
                httpNodeC
        );

        nodeC.start();
        waitForNodesCount(3);

        String jwtToken = IndexerJwtAuthTokenProvider.createToken(DatanodeContainerizedBackend.SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), Duration.seconds(600));
        OpensearchTestIndexCreation osIndexClient = new OpensearchTestIndexCreation(RestOperationParameters.builder()
                .port(nodeA.getOpensearchRestPort())
                .truststore(trustStore)
                .jwtToken(jwtToken)
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
                .username(restAdminUsername)
                .password(ContainerizedGraylogBackend.ROOT_PASSWORD_PLAINTEXT)
                .build();
        new DatanodeRestApiWait(datanodeRestParameters)
                .waitForAvailableStatus();
        new DatanodeStatusChangeOperation(datanodeRestParameters)
                .triggerNodeRemoval();


        // check that primary shard node is gone and there are still a primary and a secondary
        waitForNodesCount(replica.get(), 2);
        jwtToken = IndexerJwtAuthTokenProvider.createToken(DatanodeContainerizedBackend.SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), Duration.seconds(600));
        osIndexClient = new OpensearchTestIndexCreation(RestOperationParameters.builder()
                .port(replica.get().getOpensearchRestPort())
                .truststore(trustStore)
                .jwtToken(jwtToken)
                .build());
        List<String> newShardNodes = osIndexClient.getShardNodes();
        Assertions.assertEquals(newShardNodes.size(), 2);
        Assertions.assertFalse(newShardNodes.contains(primary.get().getNodeName()));
    }

    @NotNull
    private DatanodeContainerizedBackend createDatanodeContainer(Network network,
                                                                 MongoDBTestService mongodb,
                                                                 String hostname,
                                                                 KeystoreInformation transportKeystore,
                                                                 KeystoreInformation httpKeystore) {
        return new DatanodeContainerizedBackend(
                network,
                mongodb,
                hostname,
                datanodeContainer -> {
                    datanodeContainer.withNetwork(network);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", DatanodeContainerizedBackend.SIGNING_SECRET);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_CLUSTER_INITIAL_MANAGER_NODES", hostnameNodeA);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", hostnameNodeA + ":9300");

                    datanodeContainer.withFileSystemBind(transportKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-transport-certificates.p12");
                    datanodeContainer.withFileSystemBind(httpKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-https-certificates.p12");

                    // configure transport security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", transportKeystore.passwordAsString());
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");

                    // configure http security
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", httpKeystore.passwordAsString());

                    // this is the interface that we bind opensearch to. It must be 0.0.0.0 if we want
                    // to be able to reach opensearch from outside the container and docker network (true?)
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

                    // HOSTNAME is used to generate the SSL certificates and to communicate inside the
                    // container and docker network, where we do the hostname validation.
                    datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(hostname));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", hostname);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_MONGODB_URI", mongodb.internalUri());

                    datanodeContainer.withEnv("GRAYLOG_DATANODE_ROOT_USERNAME", restAdminUsername);

                });
    }


    private void waitForNodesCount(final int countOfNodes) throws ExecutionException, RetryException {
        waitForNodesCount(nodeA, countOfNodes);
    }

    private void waitForNodesCount(DatanodeContainerizedBackend node, final int countOfNodes) throws ExecutionException, RetryException {
        final String jwtToken = IndexerJwtAuthTokenProvider.createToken(DatanodeContainerizedBackend.SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), Duration.seconds(120));

        try {
            new DatanodeOpensearchWait(RestOperationParameters.builder()
                    .port(node.getOpensearchRestPort())
                    .truststore(trustStore)
                    .jwtToken(jwtToken)
                    .build())
                    .waitForNodesCount(countOfNodes);

        } catch (Exception retryException) {
            LOG.error("DataNode Container logs from nodeA follow:\n" + nodeA.getLogs());
            LOG.error("DataNode Container logs from node B follow:\n" + nodeB.getLogs());
            throw retryException;
        }
    }
}
