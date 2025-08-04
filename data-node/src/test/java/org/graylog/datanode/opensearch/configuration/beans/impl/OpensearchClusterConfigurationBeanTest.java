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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.TestDataNodeNodeClusterService;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class OpensearchClusterConfigurationBeanTest {

    final TestDataNodeNodeClusterService testNodeService = new TestDataNodeNodeClusterService();

    @BeforeEach
    void setUp() {
        testNodeService.registerServer(DataNodeDto.builder()
                .setId(Tools.generateServerId())
                .setTransportAddress("https://my_manager_node:9200")
                .setHostname("my_manager_node")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setOpensearchRoles(List.of(OpensearchNodeRole.CLUSTER_MANAGER, OpensearchNodeRole.DATA))
                .build());

        testNodeService.registerServer(DataNodeDto.builder()
                .setId(Tools.generateServerId())
                .setTransportAddress("https://my_other_manager_node:9200")
                .setHostname("my_other_manager_node")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setOpensearchRoles(List.of(OpensearchNodeRole.CLUSTER_MANAGER, OpensearchNodeRole.INGEST))
                .build());

        testNodeService.registerServer(DataNodeDto.builder()
                .setId(Tools.generateServerId())
                .setTransportAddress("https://my_search_node:9200")
                .setHostname("my_search_node")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setOpensearchRoles(List.of(OpensearchNodeRole.SEARCH))
                .build());
    }

    @Test
    void testManagerNodes() throws ValidationException, RepositoryException {
        final OpensearchClusterConfigurationBean configurationBean = new OpensearchClusterConfigurationBean(DatanodeTestUtils.datanodeConfiguration(
                Map.of("hostname", "this_node_can_be_manager", "node_roles", OpensearchNodeRole.CLUSTER_MANAGER)), testNodeService);

        final DatanodeConfigurationPart configurationPart = configurationBean.buildConfigurationPart(new OpensearchConfigurationParams(Collections.emptyList(), Map.of()));

        // initial cluster manager nodes should only contain nodes that publish cluster_manager role, ignore all other nodes
        final String initialManagerNodes = configurationPart.properties().get("cluster.initial_cluster_manager_nodes");
        Assertions.assertThat(initialManagerNodes).isNotEmpty();
        final List<String> managerNodes = Arrays.asList(initialManagerNodes.split(","));

        Assertions.assertThat(managerNodes)
                .containsOnly("my_manager_node", "my_other_manager_node", "this_node_can_be_manager");
    }

    @Test
    void testManagerNodesWithSelfNoManager() throws ValidationException, RepositoryException {
        final OpensearchClusterConfigurationBean configurationBean = new OpensearchClusterConfigurationBean(DatanodeTestUtils.datanodeConfiguration(
                Map.of("hostname", "this_node_cannot_be_manager", "node_roles", OpensearchNodeRole.SEARCH)), testNodeService);

        final DatanodeConfigurationPart configurationPart = configurationBean.buildConfigurationPart(new OpensearchConfigurationParams(Collections.emptyList(), Map.of()));

        // initial cluster manager nodes should only contain nodes that publish cluster_manager role, ignore all other nodes
        final String initialManagerNodes = configurationPart.properties().get("cluster.initial_cluster_manager_nodes");
        Assertions.assertThat(initialManagerNodes).isNotEmpty();
        final List<String> managerNodes = Arrays.asList(initialManagerNodes.split(","));

        Assertions.assertThat(managerNodes)
                .containsOnly("my_manager_node", "my_other_manager_node");
    }

}
