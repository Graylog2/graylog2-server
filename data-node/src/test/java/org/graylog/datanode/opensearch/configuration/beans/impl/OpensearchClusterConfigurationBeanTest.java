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
import org.graylog.datanode.opensearch.configuration.OpensearchSeedHostsResolver;
import org.graylog.datanode.opensearch.configuration.UnicastHostsFile;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.files.TextConfigFile;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.TestDataNodeNodeClusterService;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class OpensearchClusterConfigurationBeanTest {

    final TestDataNodeNodeClusterService testNodeService = new TestDataNodeNodeClusterService();

    @BeforeEach
    void setUp() {
        testNodeService.registerServer(DataNodeDto.builder()
                .setId(Tools.generateServerId())
                .setTransportAddress("https://my_manager_node:9200")
                .setClusterAddress("my_manager_node:9300")
                .setHostname("my_manager_node")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setOpensearchRoles(List.of(OpensearchNodeRole.CLUSTER_MANAGER, OpensearchNodeRole.DATA))
                .build());

        testNodeService.registerServer(DataNodeDto.builder()
                .setId(Tools.generateServerId())
                .setTransportAddress("https://my_other_manager_node:9200")
                .setClusterAddress("my_other_manager_node:9300")
                .setHostname("my_other_manager_node")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setOpensearchRoles(List.of(OpensearchNodeRole.CLUSTER_MANAGER, OpensearchNodeRole.INGEST))
                .build());

        testNodeService.registerServer(DataNodeDto.builder()
                .setId(Tools.generateServerId())
                .setTransportAddress("https://my_warm_node:9200")
                .setHostname("my_search_node")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setOpensearchRoles(List.of("search"))
                .build());
    }

    @Test
    void testSeedHostsFile(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final OpensearchClusterConfigurationBean configurationBean = new OpensearchClusterConfigurationBean(DatanodeTestUtils.datanodeConfiguration(
                Map.of("hostname", "this_node"), tempDir), new OpensearchSeedHostsResolver(testNodeService));

        final DatanodeConfigurationPart configurationPart = configurationBean.buildConfigurationPart(new OpensearchConfigurationParams(DatanodeTestUtils.mockDatanodeConfiguration(tempDir), tempDir));

        Assertions.assertThat(configurationPart.properties()).containsEntry("discovery.seed_providers", "file");
        Assertions.assertThat(configurationPart.properties()).containsEntry("cluster.default_number_of_replicas", "1");
        // resolved separately, only for the node bootstrapping the cluster
        Assertions.assertThat(configurationPart.properties()).doesNotContainKey("cluster.initial_cluster_manager_nodes");

        // nodes without a cluster address are ignored, the rest is sorted
        Assertions.assertThat(configurationPart.configFiles())
                .filteredOn(f -> f.relativePath().equals(UnicastHostsFile.FILENAME))
                .singleElement()
                .isInstanceOfSatisfying(TextConfigFile.class, f -> Assertions.assertThat(f.text())
                        .isEqualTo("my_manager_node:9300\nmy_other_manager_node:9300"));
    }
}
