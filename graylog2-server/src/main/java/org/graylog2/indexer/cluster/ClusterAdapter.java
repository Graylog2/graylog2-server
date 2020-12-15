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

import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.system.stats.elasticsearch.ClusterStats;
import org.graylog2.system.stats.elasticsearch.ShardStats;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ClusterAdapter {
    Optional<HealthStatus> health(Collection<String> indices);

    Set<NodeFileDescriptorStats> fileDescriptorStats();

    Set<NodeDiskUsageStats> diskUsageStats();

    ClusterAllocationDiskSettings clusterAllocationDiskSettings();

    Optional<String> nodeIdToName(String nodeId);

    Optional<String> nodeIdToHostName(String nodeId);

    boolean isConnected();

    Optional<String> clusterName(Collection<String> indices);

    Optional<ClusterHealth> clusterHealthStats(Collection<String> indices);

    ClusterStats clusterStats();

    PendingTasksStats pendingTasks();

    ShardStats shardStats(Collection<String> indices);
}
