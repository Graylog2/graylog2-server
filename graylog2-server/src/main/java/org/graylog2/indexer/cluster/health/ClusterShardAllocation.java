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
package org.graylog2.indexer.cluster.health;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shard usage against the cluster shard limit. OpenSearch enforces {@code cluster.max_shards_per_node} cluster-wide: all
 * primary and replica shards of open indices, assigned or not, are checked against the setting multiplied by the
 * number of data nodes, capped by {@code cluster.routing.allocation.total_shards_limit} if that is set.
 *
 * @param maxShardsPerNode     the {@code cluster.max_shards_per_node} setting
 * @param maxShardsPerCluster  the {@code cluster.routing.allocation.total_shards_limit} setting, {@code -1} if unset
 * @param nodeShardAllocations assigned shards per data node
 * @param unassignedShards     shards no data node holds
 * @param openShards           primary and replica shards of the open indices the limit applies to
 */
public record ClusterShardAllocation(int maxShardsPerNode,
                                     int maxShardsPerCluster,
                                     List<NodeShardAllocation> nodeShardAllocations,
                                     int unassignedShards,
                                     long openShards) {
    public static final int NO_CLUSTER_LIMIT = -1;
    // _cat/allocation reports the shards no node holds in a row under this pseudo node name.
    private static final String UNASSIGNED_NODE = "UNASSIGNED";

    public ClusterShardAllocation(int maxShardsPerNode, List<NodeShardAllocation> nodeShardAllocations) {
        this(maxShardsPerNode, NO_CLUSTER_LIMIT, nodeShardAllocations, 0, sumShards(nodeShardAllocations));
    }

    /**
     * Builds the allocation from {@code _cat/allocation} rows, moving the {@code UNASSIGNED} row out of the per-node
     * list into {@link #unassignedShards()}. The remaining rows are the data nodes, {@code _cat/allocation} lists
     * the same nodes OpenSearch counts for the limit.
     */
    public static ClusterShardAllocation fromCatAllocation(int maxShardsPerNode,
                                                           int maxShardsPerCluster,
                                                           List<NodeShardAllocation> catAllocationRows,
                                                           long openShards) {
        final Map<Boolean, List<NodeShardAllocation>> rowsByUnassigned = catAllocationRows.stream()
                .collect(Collectors.partitioningBy(row -> UNASSIGNED_NODE.equals(row.node())));
        return new ClusterShardAllocation(
                maxShardsPerNode,
                maxShardsPerCluster,
                rowsByUnassigned.get(false),
                (int) sumShards(rowsByUnassigned.get(true)),
                openShards);
    }

    /**
     * Like {@link #fromCatAllocation(int, int, List, long)} for backends that don't read the open index shards or the
     * cluster limit. Counts all rows instead, which includes the shards of closed indices.
     */
    public static ClusterShardAllocation fromCatAllocation(int maxShardsPerNode, List<NodeShardAllocation> catAllocationRows) {
        return fromCatAllocation(maxShardsPerNode, NO_CLUSTER_LIMIT, catAllocationRows, sumShards(catAllocationRows));
    }

    public int dataNodes() {
        return nodeShardAllocations.size();
    }

    /**
     * The cluster-wide shard limit: {@code cluster.max_shards_per_node} multiplied by the number of data nodes, or
     * {@code cluster.routing.allocation.total_shards_limit} if that is lower.
     */
    public long maxShards() {
        final long maxShardsOfDataNodes = (long) maxShardsPerNode * dataNodes();
        return maxShardsPerCluster == NO_CLUSTER_LIMIT ? maxShardsOfDataNodes : Math.min(maxShardsPerCluster, maxShardsOfDataNodes);
    }

    private static long sumShards(List<NodeShardAllocation> allocations) {
        return allocations.stream().mapToLong(NodeShardAllocation::shards).sum();
    }
}
