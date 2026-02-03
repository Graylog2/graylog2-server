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
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShardsMetricsSupplierTest {
    @Mock
    private ElasticsearchConfiguration elasticsearchConfiguration;

    @Mock
    private Cluster cluster;

    @InjectMocks
    private ShardsMetricsSupplier shardsMetricsSupplier;

    @Test
    void shouldReturnShardMetrics() {
        Size minSize = Size.gigabytes(29L);
        Size maxSize = Size.gigabytes(34L);
        ClusterHealth.ShardStatus shardStatus = ClusterHealth.ShardStatus.create(5, 4, 2, 1);

        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize()).thenReturn(minSize);
        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize()).thenReturn(maxSize);
        when(cluster.clusterHealthStats()).thenReturn(Optional.of(
                ClusterHealth.create("yellow", shardStatus)
        ));

        Optional<TelemetryEvent> event = shardsMetricsSupplier.get();

        assertThat(event).isPresent();
        assertThat(event.get().metrics()).isEqualTo(Map.<String, Object>of(
                "shard_min_size", minSize.getQuantity(),
                "shard_max_size", maxSize.getQuantity(),
                "shards_active", shardStatus.active(),
                "shards_initializing", shardStatus.initializing(),
                "shards_relocating", shardStatus.relocating(),
                "shards_unassigned", shardStatus.unassigned()
        ));
    }

    @Test
    void shouldReturnShardMetricsWithFallbackValues() {
        Optional<TelemetryEvent> event = shardsMetricsSupplier.get();

        assertThat(event).isPresent();
        assertThat(event.get().metrics()).isEqualTo(Map.<String, Object>of(
                "shard_min_size", 0L,
                "shard_max_size", 0L,
                "shards_active", 0,
                "shards_initializing", 0,
                "shards_relocating", 0,
                "shards_unassigned", 0
        ));
    }
}
