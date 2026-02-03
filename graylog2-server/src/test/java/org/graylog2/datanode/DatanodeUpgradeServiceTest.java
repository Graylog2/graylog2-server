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
package org.graylog2.datanode;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.plugins.datanode.dto.FlushResponse;
import org.graylog.plugins.datanode.dto.ManagerNode;
import org.graylog.plugins.datanode.dto.Node;
import org.graylog.plugins.datanode.dto.ShardReplication;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.nodes.TestDataNodeNodeClusterService;
import org.graylog2.plugin.Version;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class DatanodeUpgradeServiceTest {

    @Test
    void testOutdatedNodes() {
        final NodeService<DataNodeDto> nodeService = new TestDataNodeNodeClusterService();

        nodeService.registerServer(buildTestNode("1", DataNodeStatus.AVAILABLE, Version.from(6, 1, 0), true));
        nodeService.registerServer(buildTestNode("2", DataNodeStatus.AVAILABLE, Version.from(6, 1, 11), false));
        nodeService.registerServer(buildTestNode("3", DataNodeStatus.AVAILABLE, Version.from(6, 2, 1), false));

        final DatanodeUpgradeService upgradeService = new DatanodeUpgradeService(
                mockUpgradeAdapter(nodeService, "GREEN", ShardReplication.ALL),
                nodeService,
                Version.from(6, 2, 1));

        final DatanodeUpgradeStatus status = upgradeService.status();

        Assertions.assertThat(status.upToDateNodes())
                .hasSize(1)
                .map(DataNodeInformation::nodeName)
                .contains("datanode-3");

        Assertions.assertThat(status.outdatedNodes())
                .hasSize(2)
                .map(DataNodeInformation::nodeName)
                .containsExactly("datanode-2", "datanode-1"); // datanode-1 is manager, should go last

        Assertions.assertThat(status.outdatedNodes())
                // there is only one node that has the upgradePossible flag, it can't be manager if there are at least 2 nodes
                .filteredOn(DataNodeInformation::upgradePossible)
                .hasSize(1)
                .map(DataNodeInformation::nodeName)
                .contains("datanode-2");

    }

    @Test
    void testTooNewDatanode() {
        // notice that the datanode version is higher than the server version. This is something we generally neither expect
        // nor want, but it happens and users can get into this state.
        final Version datanodeVersion = Version.from(6, 2, 1);
        final Version serverVersion = Version.from(6, 1, 10);

        final NodeService<DataNodeDto> nodeService = new TestDataNodeNodeClusterService();
        nodeService.registerServer(buildTestNode("1", DataNodeStatus.AVAILABLE, datanodeVersion, true));

        final DatanodeUpgradeService upgradeService = new DatanodeUpgradeService(
                mockUpgradeAdapter(nodeService, "GREEN", ShardReplication.ALL),
                nodeService,
                serverVersion);

        final DatanodeUpgradeStatus status = upgradeService.status();
        Assertions.assertThat(status.upToDateNodes())
                .hasSize(1)
                .map(DataNodeInformation::nodeName)
                .contains("datanode-1");

        Assertions.assertThat(status.warnings())
                .hasSize(1)
                .contains("Your data node datanode-1 is running a newer version <6.2.1> than your server <6.1.10>. You should update your server first.");


        Assertions.assertThat(status.outdatedNodes())
                .isEmpty();
    }

    @Test
    void testOutdatedSorting() {
        final NodeService<DataNodeDto> nodeService = new TestDataNodeNodeClusterService();

        nodeService.registerServer(buildTestNode("2", DataNodeStatus.AVAILABLE, Version.from(6, 1, 0), false));
        nodeService.registerServer(buildTestNode("1", DataNodeStatus.AVAILABLE, Version.from(6, 1, 0), false));
        nodeService.registerServer(buildTestNode("4", DataNodeStatus.AVAILABLE, Version.from(6, 1, 0), false));
        nodeService.registerServer(buildTestNode("3", DataNodeStatus.AVAILABLE, Version.from(6, 1, 0), true));

        final DatanodeUpgradeService upgradeService = new DatanodeUpgradeService(
                mockUpgradeAdapter(nodeService, "GREEN", ShardReplication.ALL),
                nodeService,
                Version.from(6, 2, 1));

        final DatanodeUpgradeStatus status = upgradeService.status();

        Assertions.assertThat(status.upToDateNodes())
                .isEmpty();

        Assertions.assertThat(status.outdatedNodes())
                .hasSize(4);


        Assertions.assertThat(status.outdatedNodes())
                .filteredOn(DataNodeInformation::upgradePossible)
                .hasSize(1)
                .map(DataNodeInformation::nodeName)
                .doesNotContain("datanode-3"); // because it's a manager

        Assertions.assertThat(status.outdatedNodes().get(3)) // the last node should be the manager
                .extracting(DataNodeInformation::nodeName)
                .isEqualTo("datanode-3");
    }

    private DataNodeDto buildTestNode(String nodeId, DataNodeStatus status, Version version, boolean manager) {
        final String nodeName = "datanode-" + nodeId;
        return DataNodeDto.Builder.builder()
                .setId(nodeId)
                .setHostname(nodeName)
                .setClusterAddress("http://" + nodeName + ":9300")
                .setTransportAddress("http://" + nodeName + ":9200")
                .setLeader(manager)
                .setDataNodeStatus(status)
                .setDatanodeVersion(version.toString())
                .build();
    }

    @Nonnull
    private static DatanodeUpgradeServiceAdapter mockUpgradeAdapter(NodeService<DataNodeDto> nodeService, final String clusterStatus, final ShardReplication shardReplication) {
        final List<Node> nodes = nodeService.allActive().values().stream().map(DatanodeUpgradeServiceTest::toOpensearchNode).collect(Collectors.toList());
        final ManagerNode managerNode = nodeService.allActive().values().stream().filter(NodeDto::isLeader).findFirst().map(n -> new ManagerNode(n.getNodeId(), n.getHostname())).orElseThrow(() -> new IllegalStateException("No manager node found"));
        return new DatanodeUpgradeServiceAdapter() {
            @Override
            public ClusterState getClusterState() {
                return new ClusterState(
                        clusterStatus,
                        "junit-cluster",
                        nodes.size(),
                        nodes.size(),
                        0,
                        0,
                        0,
                        nodes.size(),
                        0,
                        shardReplication,
                        managerNode,
                        nodes
                );
            }

            @Override
            public void disableShardReplication() {

            }

            @Override
            public void enableShardReplication() {

            }

            @Override
            public FlushResponse flush() {
                return new FlushResponse(0, 0, 0);
            }
        };
    }

    @Nonnull
    private static Node toOpensearchNode(DataNodeDto n) {
        return new Node(n.getHostname(), "192.168.100." + n.id(), n.getHostname(), "2.15.0", Collections.emptyList());
    }
}
