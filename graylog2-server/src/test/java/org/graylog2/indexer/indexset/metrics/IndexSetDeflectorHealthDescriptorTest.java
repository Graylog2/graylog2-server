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
package org.graylog2.indexer.indexset.metrics;

import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.metrics.entity.EntityMetric;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexSetDeflectorHealthDescriptorTest {
    private static final String A = "000000000000000000000001";
    private static final String B = "000000000000000000000002";
    private final ClusterAdapter clusterAdapter = mock(ClusterAdapter.class);
    private final IndexSetService indexSetService = mock(IndexSetService.class);
    private final IndexSetRegistry indexSetRegistry = mock(IndexSetRegistry.class);
    private final IndexSetDeflectorHealthDescriptor descriptor = new IndexSetDeflectorHealthDescriptor(
            clusterAdapter, indexSetService, indexSetRegistry, Duration.ofMinutes(1));

    @Test
    void assignsTheReducedHealthToWritableIndexSetsOnly() {
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry,
                IndexSetMocks.indexSet(A, "graylog", true),
                IndexSetMocks.indexSet(B, "archive", false));
        when(clusterAdapter.deflectorHealth(List.of("graylog_deflector"))).thenReturn(Optional.of(HealthStatus.Yellow));

        assertThat(descriptor.compute(List.of(A, B))).containsExactlyInAnyOrder(
                new EntityMetric<>(A, HealthStatus.Yellow),
                new EntityMetric<>(B, null));
    }

    @Test
    void reportsNoValueWhenTheHealthIsUnknown() {
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry, IndexSetMocks.indexSet(A, "graylog", true));
        when(clusterAdapter.deflectorHealth(List.of("graylog_deflector"))).thenReturn(Optional.empty());

        assertThat(descriptor.compute(List.of(A))).containsExactly(new EntityMetric<>(A, null));
    }
    @Test
    void reportsNoValueWhenTheLookupFails() {
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry, IndexSetMocks.indexSet(A, "graylog", true));
        when(clusterAdapter.deflectorHealth(List.of("graylog_deflector")))
                .thenThrow(new IllegalStateException("Duplicate key graylog_deflector"));

        assertThat(descriptor.compute(List.of(A))).containsExactly(new EntityMetric<>(A, null));
    }
}
