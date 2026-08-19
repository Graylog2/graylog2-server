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
package org.graylog2.indexer.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.ClusterShardAllocation;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.system.stats.elasticsearch.ClusterStats;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.system.stats.elasticsearch.NodeOSInfo;
import org.graylog2.system.stats.elasticsearch.NodeUtilization;
import org.graylog2.system.stats.elasticsearch.ShardStats;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ClusterAdapter {
    Optional<HealthStatus> health();

    Set<NodeFileDescriptorStats> fileDescriptorStats();

    ClusterShardAllocation clusterShardAllocation();

    Set<NodeDiskUsageStats> diskUsageStats();

    ClusterAllocationDiskSettings clusterAllocationDiskSettings();

    /**
     * The cluster-level {@code search.query.max_query_string_length} limit, if the backend registers it. A
     * query string longer than this is rejected by the backend. Empty when the setting does not exist
     * (Elasticsearch never registers it) or cannot be parsed, in which case callers must skip the check
     * rather than assume a default - the value is admin-changeable at runtime.
     */
    Optional<Integer> maxQueryStringLength();

    /**
     * Cluster settings arrive as strings and may be absent (empty string). Shared by the storage backends so
     * they agree on what an unusable value means: no limit information, not a guessed default.
     */
    static Optional<Integer> parseSettingAsPositiveInt(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            final int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    Optional<String> nodeIdToName(String nodeId);

    Optional<String> nodeIdToHostName(String nodeId);

    boolean isConnected();

    Optional<String> clusterName();

    Optional<ClusterHealth> clusterHealthStats();

    ClusterStats clusterStats();

    JsonNode rawClusterStats();

    PendingTasksStats pendingTasks();

    Map<String, NodeInfo> nodesInfo();

    Map<String, NodeOSInfo> nodesHostInfo();

    /**
     * Live per-node runtime utilization ({@code _nodes/stats/os,jvm}): CPU percent and JVM heap-used percent, keyed
     * by node id. A single bounded round-trip; the search-cluster health reporters sample and window this on the leader.
     */
    Map<String, NodeUtilization> nodesUtilization();

    ShardStats shardStats();

    /**
     * The cluster health response has no such field, so implementations derive it from each node's roles.
     */
    int countOfClusterManagerEligibleNodes();

    Optional<HealthStatus> deflectorHealth(Collection<String> indices);
}
