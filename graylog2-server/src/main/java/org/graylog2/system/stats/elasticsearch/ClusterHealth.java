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
package org.graylog2.system.stats.elasticsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.cluster.PendingTasksStats;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ClusterHealth {
    @JsonProperty
    public abstract int numberOfNodes();

    @JsonProperty
    public abstract int numberOfDataNodes();

    @JsonProperty
    public abstract int activeShards();

    @JsonProperty
    public abstract int relocatingShards();

    @JsonProperty
    public abstract int activePrimaryShards();

    @JsonProperty
    public abstract int initializingShards();

    @JsonProperty
    public abstract int unassignedShards();

    @JsonProperty
    public abstract boolean timedOut();

    @JsonProperty
    public abstract int pendingTasks();

    @JsonProperty
    public abstract List<Long> pendingTasksTimeInQueue();

    public static ClusterHealth from(ShardStats shardStats, PendingTasksStats pendingTasksStats) {
        return create(
                shardStats.numberOfNodes(),
                shardStats.numberOfDataNodes(),
                shardStats.activeShards(),
                shardStats.relocatingShards(),
                shardStats.activePrimaryShards(),
                shardStats.initializingShards(),
                shardStats.unassignedShards(),
                shardStats.timedOut(),
                pendingTasksStats.pendingTasks(),
                pendingTasksStats.pendingTasksTimeInQueue()
        );
    }

    public static ClusterHealth create(int numberOfNodes,
                                       int numberOfDataNodes,
                                       int activeShards,
                                       int relocatingShards,
                                       int activePrimaryShards,
                                       int initializingShards,
                                       int unassignedShards,
                                       boolean timedOut,
                                       int pendingTasks,
                                       List<Long> pendingTasksTimeInQueue) {
        return new AutoValue_ClusterHealth(numberOfNodes, numberOfDataNodes, activeShards, relocatingShards,
                activePrimaryShards, initializingShards, unassignedShards, timedOut,
                pendingTasks, pendingTasksTimeInQueue);
    }
}
