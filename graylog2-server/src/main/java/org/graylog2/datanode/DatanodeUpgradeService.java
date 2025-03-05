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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.plugins.datanode.dto.FlushResponse;
import org.graylog.plugins.datanode.dto.Node;
import org.graylog.plugins.datanode.dto.ShardReplication;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.Version;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class DatanodeUpgradeService {

    public static final String CLUSTER_MANAGER_ROLE = "cluster_manager";
    private final DatanodeUpgradeServiceAdapter upgradeService;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public DatanodeUpgradeService(DatanodeUpgradeServiceAdapter upgradeService, NodeService<DataNodeDto> nodeService) {
        this.upgradeService = upgradeService;
        this.nodeService = nodeService;
    }

    public DatanodeUpgradeStatus status() {
        //final Version serverVersion = Version.CURRENT_CLASSPATH;
        final Version serverVersion = new Version(com.github.zafarkhaja.semver.Version.parse("6.2.0"));


        final ClusterState clusterState = upgradeService.getClusterState();
        final Collection<DataNodeDto> dataNodes = nodeService.allActive().values();

        final Set<DataNodeDto> upToDateDataNodes = dataNodes.stream().filter(n -> isVersionEqualIgnoreBuildMetadata(n.getDatanodeVersion(), serverVersion)).collect(Collectors.toSet());
        final Set<DataNodeDto> toUpgradeDataNodes = dataNodes.stream().filter(n -> !upToDateDataNodes.contains(n)).collect(Collectors.toSet());

        final boolean clusterReadyForUpgrade = clusterState.status().equals("GREEN") && clusterState.shardReplication() == ShardReplication.ALL && clusterState.relocatingShards() == 0;

        return new DatanodeUpgradeStatus(serverVersion,
                clusterState,
                clusterReadyForUpgrade,
                enrichData(upToDateDataNodes, clusterState, serverVersion, clusterReadyForUpgrade),
                enrichData(toUpgradeDataNodes, clusterState, serverVersion, clusterReadyForUpgrade)
        );
    }

    private List<DataNodeInformation> enrichData(Set<DataNodeDto> toUpgradeDataNodes, ClusterState clusterState, Version serverVersion, boolean clusterReadyForUpgrade) {
        return toUpgradeDataNodes.stream().map(n -> remix(n, toUpgradeDataNodes, clusterState, serverVersion, clusterReadyForUpgrade)).collect(Collectors.toList());
    }

    @Nonnull
    private static DataNodeInformation remix(DataNodeDto node, Set<DataNodeDto> toUpgradeDataNodes, ClusterState clusterState, Version serverVersion, boolean clusterReadyForUpgrade) {
        final Optional<Node> opensearchInformation = clusterState.nodes().stream().filter(n -> n.host().equals(node.getHostname())).findFirst();

        final String nodeName = clusterState.getName(node.getHostname());

        final boolean isManagerNode = clusterState.managerNode().name().equals(nodeName);
        final boolean isLatestVersion = isVersionEqualIgnoreBuildMetadata(node.getDatanodeVersion(), serverVersion);
        boolean upgradePossible = clusterReadyForUpgrade && !isLatestVersion && (!isManagerNode || toUpgradeDataNodes.size() == 1);

        return new DataNodeInformation(
                nodeName,
                node.getDataNodeStatus(),
                node.getDatanodeVersion(),
                node.getHostname(),
                opensearchInformation.map(Node::ip).orElse(null),
                opensearchInformation.map(Node::version).orElse(null),
                opensearchInformation.map(Node::roles).orElse(null),
                upgradePossible,
                isManagerNode);
    }

    protected static boolean isVersionEqualIgnoreBuildMetadata(String datanodeVersion, Version serverVersion) {
        final com.github.zafarkhaja.semver.Version datanode = com.github.zafarkhaja.semver.Version.parse(datanodeVersion);
        return serverVersion.getVersion().compareToIgnoreBuildMetadata(datanode) == 0;
    }

    public FlushResponse stopSync() {
        upgradeService.disableShardReplication();
        return upgradeService.flush();
    }

    public FlushResponse startSync() {
        upgradeService.enableShardReplication();
        return upgradeService.flush();
    }
}
