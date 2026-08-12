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
package org.graylog2.cluster.nodes.opensearch;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.system.stats.elasticsearch.NodeUtilization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OpensearchNodesProvider implements Provider<List<OpensearchNode>> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchNodesProvider.class);

    private final Cluster cluster;

    @Inject
    public OpensearchNodesProvider(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public List<OpensearchNode> get() {
        try {
            final Map<String, NodeInfo> nodesInfo = cluster.getNodesInfo();
            final Map<String, NodeUtilization> utilizationByName = cluster.getNodesUtilization().values().stream()
                    .collect(Collectors.toMap(NodeUtilization::name, Function.identity(), (a, b) -> a));
            final Map<String, NodeDiskUsageStats> diskUsageByName = cluster.getDiskUsageStats().stream()
                    .collect(Collectors.toMap(NodeDiskUsageStats::name, Function.identity(), (a, b) -> a));

            return nodesInfo.entrySet().stream()
                    .map(entry -> toOpensearchNode(entry.getKey(), entry.getValue(), utilizationByName, diskUsageByName))
                    .toList();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not get OpenSearch nodes, returning fallback.", e);
            } else {
                LOG.warn("Could not get OpenSearch nodes, returning fallback. Reason: {}", e.getMessage());
            }
            return List.of();
        }
    }

    private OpensearchNode toOpensearchNode(String id, NodeInfo info,
                                            Map<String, NodeUtilization> utilizationByName,
                                            Map<String, NodeDiskUsageStats> diskUsageByName) {
        final NodeUtilization utilization = utilizationByName.get(info.name());
        final NodeDiskUsageStats diskUsage = diskUsageByName.get(info.name());
        return new OpensearchNode(
                id,
                info.name(),
                info.version(),
                info.roles(),
                info.jvmMemHeapMaxInBytes(),
                utilization == null ? null : utilization.jvmHeapUsedPercent(),
                utilization == null ? null : utilization.cpuPercent(),
                diskUsage == null ? null : diskUsage.diskUsedPercent(),
                diskUsage == null ? null : diskUsage.diskUsed().getBytes(),
                diskUsage == null ? null : diskUsage.diskTotal().getBytes()
        );
    }
}
