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

import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.restoperations.DatanodeOpensearchWait;
import org.graylog.testing.restoperations.RestOperationParameters;
import org.junit.jupiter.api.AfterEach;
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
import java.util.stream.Stream;

import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;

/**
 * Data nodes started at the same time, without explicitly configured initial cluster manager nodes and seed hosts,
 * have to form one cluster instead of each bootstrapping its own.
 */
public class DatanodeClusterBootstrapIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeClusterBootstrapIT.class);

    @TempDir
    static Path tempDir;

    private DatanodeContainerizedBackend nodeA;
    private DatanodeContainerizedBackend nodeB;
    private KeyStore trustStore;
    private Network network;
    private MongoDBTestService mongoDBTestService;

    @BeforeEach
    void setUp() throws GeneralSecurityException, IOException {
        final FilesystemKeystoreInformation ca = DatanodeSecurityTestUtils.generateCa(tempDir);
        trustStore = DatanodeSecurityTestUtils.buildTruststore(ca);

        network = Network.newNetwork();
        mongoDBTestService = MongoDBTestService.createStarted(network);

        nodeA = createDatanodeContainer(ca, "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef"));
        nodeB = createDatanodeContainer(ca, "graylog-datanode-host-" + RandomStringUtils.random(8, "0123456789abcdef"));
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
    void simultaneouslyStartedNodesFormOneCluster() throws Exception {
        Stream.of(nodeA, nodeB).parallel().forEach(DatanodeContainerizedBackend::start);

        // both nodes see two nodes, so they are members of the same cluster
        waitForGreenStatusAndNodesCount(nodeA, 2);
        waitForGreenStatusAndNodesCount(nodeB, 2);
    }

    private DatanodeContainerizedBackend createDatanodeContainer(FilesystemKeystoreInformation ca, String hostname) throws GeneralSecurityException, IOException {
        final FilesystemKeystoreInformation transportKeystore = DatanodeSecurityTestUtils.generateTransportCert(tempDir, ca, hostname);
        final FilesystemKeystoreInformation httpKeystore = DatanodeSecurityTestUtils.generateHttpCert(tempDir, ca, hostname);
        return new DatanodeContainerizedBackend(
                network,
                mongoDBTestService,
                hostname,
                datanodeContainer -> {
                    datanodeContainer.withNetwork(network);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", DatanodeContainerizedBackend.SIGNING_SECRET);

                    // no static cluster configuration, everything has to be resolved from the registered data nodes
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_INITIAL_CLUSTER_MANAGER_NODES", "");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", "");

                    datanodeContainer.withFileSystemBind(transportKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-transport-certificates.p12");
                    datanodeContainer.withFileSystemBind(httpKeystore.location().toAbsolutePath().toString(), IMAGE_WORKING_DIR + "/config/datanode-https-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE", "datanode-transport-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_TRANSPORT_CERTIFICATE_PASSWORD", new String(transportKeystore.password()));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "false");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE", "datanode-https-certificates.p12");
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_CERTIFICATE_PASSWORD", new String(httpKeystore.password()));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0");

                    datanodeContainer.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(hostname));
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_HOSTNAME", hostname);
                    datanodeContainer.withEnv("GRAYLOG_DATANODE_MONGODB_URI", mongoDBTestService.internalUri());
                });
    }

    private void waitForGreenStatusAndNodesCount(DatanodeContainerizedBackend node, int countOfNodes) throws Exception {
        try {
            new DatanodeOpensearchWait(RestOperationParameters.builder()
                    .port(node.getOpensearchRestPort())
                    .truststore(trustStore)
                    .jwtAuthToken(DatanodeContainerizedBackend.JWT_AUTH_TOKEN)
                    .build())
                    .waitForGreenStatusAndNodesCount(countOfNodes);
        } catch (Exception e) {
            LOG.error("DataNode Container logs from node A follow:\n" + nodeA.getLogs());
            LOG.error("DataNode Container logs from node B follow:\n" + nodeB.getLogs());
            throw e;
        }
    }
}
