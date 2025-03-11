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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Singleton
public class DatanodeUpgradeService {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeUpgradeService.class);

    private final DatanodeUpgradeServiceAdapter upgradeService;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public DatanodeUpgradeService(DatanodeUpgradeServiceAdapter upgradeService, NodeService<DataNodeDto> nodeService) {
        this.upgradeService = upgradeService;
        this.nodeService = nodeService;
    }

    public DatanodeUpgradeStatus status() {
        final Version serverVersion = Version.CURRENT_CLASSPATH;

        final ClusterState clusterState = upgradeService.getClusterState();
        final Collection<DataNodeDto> dataNodes = nodeService.allActive().values();

        final List<DataNodeDto> upToDateDataNodes = dataNodes.stream().filter(n -> isVersionEqualIgnoreBuildMetadata(n.getDatanodeVersion(), serverVersion)).collect(Collectors.toList());
        final List<DataNodeDto> toUpgradeDataNodes = dataNodes.stream().filter(n -> !upToDateDataNodes.contains(n)).collect(Collectors.toList());

        final boolean clusterHealthy = clusterState.status().equals("GREEN") && clusterState.relocatingShards() == 0;
        final boolean shardReplicationEnabled = clusterState.shardReplication() == ShardReplication.ALL;
        final boolean clusterReadyForUpgrade = clusterHealthy && shardReplicationEnabled;

        return new DatanodeUpgradeStatus(serverVersion,
                clusterState,
                clusterHealthy,
                shardReplicationEnabled,
                enrichData(upToDateDataNodes, clusterState, serverVersion, clusterReadyForUpgrade),
                enrichData(toUpgradeDataNodes, clusterState, serverVersion, clusterReadyForUpgrade)
        );
    }

    private List<DataNodeInformation> enrichData(List<DataNodeDto> nodes, ClusterState clusterState, Version serverVersion, boolean clusterReadyForUpgrade) {
        final Comparator<DataNodeInformation> comparator = Comparator.comparing(DataNodeInformation::upgradePossible)
                .reversed()
                .thenComparing(DataNodeInformation::nodeName);

        AtomicInteger upgradeableCounter = new AtomicInteger(1);

        return nodes.stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(DataNodeDto::getHostname)))
                .map(n -> enrichNodeInformation(n, nodes, clusterState, serverVersion, clusterReadyForUpgrade, upgradeableCounter))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Nonnull
    private static DataNodeInformation enrichNodeInformation(DataNodeDto node, List<DataNodeDto> toUpgradeDataNodes, ClusterState clusterState, Version serverVersion, boolean clusterReadyForUpgrade, AtomicInteger upgradeableCounter) {
        final Optional<Node> opensearchInformation = clusterState.findByHostname(node.getHostname());


        final boolean managerNode = opensearchInformation.map(Node::name)
                .map(n -> n.equals(clusterState.managerNode().name()))
                .orElse(false);

        final boolean isLatestVersion = isVersionEqualIgnoreBuildMetadata(node.getDatanodeVersion(), serverVersion);

        boolean upgradeTechnicallyPossible = clusterReadyForUpgrade && !isLatestVersion && (!managerNode || toUpgradeDataNodes.size() == 1);
        // we want to mark only one node as ready for upgrade, guiding the user one by one. The upgradeableCounter keeps the overview
        boolean upgradeEnabled = upgradeTechnicallyPossible && upgradeableCounter.getAndDecrement() == 1;

        return new DataNodeInformation(
                opensearchInformation.map(Node::name).orElse(node.getHostname()),
                node.getDataNodeStatus(),
                node.getDatanodeVersion(),
                node.getHostname(),
                opensearchInformation.map(Node::ip).orElse(null),
                opensearchInformation.map(Node::version).orElse(null),
                opensearchInformation.map(Node::roles).orElse(null),
                upgradeEnabled,
                managerNode);
    }

    protected static boolean isVersionEqualIgnoreBuildMetadata(String datanodeVersion, Version serverVersion) {
        final com.github.zafarkhaja.semver.Version datanode = com.github.zafarkhaja.semver.Version.parse(datanodeVersion);
        return serverVersion.getVersion().compareToIgnoreBuildMetadata(datanode) == 0;
    }

    public FlushResponse stopReplication() {
        LOG.info("Stopping shard replication");
        upgradeService.disableShardReplication();
        LOG.info("Flushing, storing all in-memory operations to segments on disk");
        return upgradeService.flush();
    }

    public FlushResponse startReplication() {
        LOG.info("Starting shard replication");
        upgradeService.enableShardReplication();
        LOG.info("Flushing, storing all in-memory operations to segments on disk");
        return upgradeService.flush();
    }
}
