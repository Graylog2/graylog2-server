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
package org.graylog2.telemetry.suppliers;

import com.github.joschi.jadconfig.util.Size;
import jakarta.inject.Inject;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.Map;
import java.util.Optional;

public class ShardsMetricsSupplier implements TelemetryMetricSupplier {
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final Cluster cluster;

    @Inject
    public ShardsMetricsSupplier(ElasticsearchConfiguration elasticsearchConfiguration, Cluster cluster) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.cluster = cluster;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        Optional<ClusterHealth.ShardStatus> shardStatus = cluster.clusterHealthStats().map(ClusterHealth::shards);

        Map<String, Object> metrics = Map.of(
                "shard_min_size", getQuantityValue(elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize()),
                "shard_max_size", getQuantityValue(elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize()),
                "shards_active", shardStatus.map(ClusterHealth.ShardStatus::active).orElse(0),
                "shards_initializing", shardStatus.map(ClusterHealth.ShardStatus::initializing).orElse(0),
                "shards_relocating", shardStatus.map(ClusterHealth.ShardStatus::relocating).orElse(0),
                "shards_unassigned", shardStatus.map(ClusterHealth.ShardStatus::unassigned).orElse(0)
        );

        return Optional.of(TelemetryEvent.of(metrics));
    }

    private Long getQuantityValue(Size timeSizeOptimizingRotationMinShardSize) {
        return Optional.ofNullable(timeSizeOptimizingRotationMinShardSize)
                .map(Size::getQuantity).orElse(0L);
    }
}
