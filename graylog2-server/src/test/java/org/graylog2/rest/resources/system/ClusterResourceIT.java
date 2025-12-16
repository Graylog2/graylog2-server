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
package org.graylog2.rest.resources.system;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.Configuration;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.ServerNodeClusterService;
import org.graylog2.cluster.nodes.ServerNodeDto;
import org.graylog2.cluster.nodes.ServerNodePaginatedService;
import org.graylog2.configuration.ConfigurationHelper;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtendWith(MongoDBExtension.class)
class ClusterResourceIT {

    private static final int STALE_LEADER_TIMEOUT_MS = 180_000;
    private ClusterResource clusterResource;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) throws ValidationException, RepositoryException {
        final Configuration configuration = configuration(Collections.singletonMap("stale_leader_timeout", String.valueOf(STALE_LEADER_TIMEOUT_MS)));
        final ServerNodeClusterService serverNodeService = new ServerNodeClusterService(mongoCollections.mongoConnection(), configuration);
        serverNodeService.registerServer(node("my-hostname", true, "5ca1ab1e-0000-4000-a000-100000000000"));
        serverNodeService.registerServer(node("aaa-hostname", false, "5ca1ab1e-0000-4000-a000-200000000000"));
        serverNodeService.registerServer(node("zzz-hostname", false, "5ca1ab1e-0000-4000-a000-300000000000"));
        final ServerNodePaginatedService serverNodePaginatedService = new ServerNodePaginatedService(mongoCollections);

        final SimpleNodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");

        clusterResource = new ClusterResource(new NodeServiceImpl(serverNodeService), Mockito.mock(ClusterConfigService.class), nodeId, serverNodePaginatedService);
    }

    @Test
    void testPaginatedNodesWithSorting() {
        Assertions.assertThat(clusterResource.nodes(1, 10, "", "hostname", SortOrder.DESCENDING).elements())
                .hasSize(3)
                .extracting(NodeDto::getHostname)
                .containsExactly("zzz-hostname", "my-hostname", "aaa-hostname");
    }


    @Test
    void testPaginatedNodesWithQueryDefaultField() {
        Assertions.assertThat(clusterResource.nodes(1, 10, "aaa", "hostname", SortOrder.ASCENDING).elements())
                .hasSize(1)
                .extracting(NodeDto::getHostname)
                .containsExactly("aaa-hostname");
    }


    @Test
    void testPaginatedNodesWithQueryDefinedField() {
        Assertions.assertThat(clusterResource.nodes(1, 10, "node_id:100000000000", "hostname", SortOrder.ASCENDING).elements())
                .hasSize(1)
                .extracting(NodeDto::getHostname)
                .containsExactly("my-hostname");
    }

    private static NodeDto node(String hostname, boolean leader, String nodeID) {
        return ServerNodeDto.Builder.builder()
                .setHostname(hostname)
                .setId(nodeID)
                .setLeader(leader)
                .setTransportAddress("http://" + hostname + ":8999")
                .setProcessing(true)
                .setLifecycle(Lifecycle.RUNNING)
                .build();
    }

    private Configuration configuration(Map<String, String> properties) throws RepositoryException, ValidationException {
        return ConfigurationHelper.initConfig(new Configuration(), properties, tempDir);
    }

}
