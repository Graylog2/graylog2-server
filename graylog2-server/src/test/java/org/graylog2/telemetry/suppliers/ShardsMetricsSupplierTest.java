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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShardsMetricsSupplierTest {
    @Mock
    private ElasticsearchConfiguration elasticsearchConfiguration;

    @Mock
    private Cluster cluster;

    @InjectMocks
    private ShardsMetricsSupplier shardsMetricsSupplier;

    @Test
    public void shouldReturnShardMetrics() {
        Size minSize = Size.gigabytes(29L);
        Size maxSize = Size.gigabytes(34L);
        ClusterHealth.ShardStatus shardStatus = ClusterHealth.ShardStatus.create(5, 4, 2, 1);

        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize()).thenReturn(minSize);
        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize()).thenReturn(maxSize);
        when(cluster.clusterHealthStats()).thenReturn(Optional.of(
                ClusterHealth.create("yellow", shardStatus)
        ));

        Optional<TelemetryEvent> event = shardsMetricsSupplier.get();

        assertTrue(event.isPresent());
        assertEquals(minSize.getQuantity(), event.get().metrics().get("shard_min_size"));
        assertEquals(maxSize.getQuantity(), event.get().metrics().get("shard_max_size"));
        assertEquals(shardStatus.active(), event.get().metrics().get("shards_active"));
        assertEquals(shardStatus.initializing(), event.get().metrics().get("shards_initializing"));
        assertEquals(shardStatus.relocating(), event.get().metrics().get("shards_relocating"));
        assertEquals(shardStatus.unassigned(), event.get().metrics().get("shards_unassigned"));
    }
}
