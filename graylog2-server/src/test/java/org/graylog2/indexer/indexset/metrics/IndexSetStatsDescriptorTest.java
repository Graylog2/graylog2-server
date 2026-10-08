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

import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.IndexSetStatsCreator;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexSetStatsDescriptorTest {
    private static final String A = "000000000000000000000001";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final IndexSetStatsCreator statsCreator = mock(IndexSetStatsCreator.class);
    private final IndexSetService indexSetService = mock(IndexSetService.class);
    private final IndexSetRegistry indexSetRegistry = mock(IndexSetRegistry.class);

    @BeforeEach
    void setUp() {
        final IndexSet indexSet = IndexSetMocks.indexSet(A, "graylog", true);
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry, indexSet);
        when(statsCreator.getForIndexSets(Set.of(indexSet))).thenReturn(Map.of(A, IndexSetStats.create(3, 15, 150)));
    }

    @Test
    void eachDescriptorReportsItsFieldOfTheBatchedStats() {
        assertThat(indexCount().compute(List.of(A))).containsExactly(new EntityMetric<>(A, 3L));
        assertThat(new IndexSetStatsDescriptor.DocumentCount(statsCreator, indexSetService, indexSetRegistry, TTL).compute(List.of(A)))
                .containsExactly(new EntityMetric<>(A, 15L));
        assertThat(new IndexSetStatsDescriptor.SizeBytes(statsCreator, indexSetService, indexSetRegistry, TTL).compute(List.of(A)))
                .containsExactly(new EntityMetric<>(A, 150L));
    }

    @Test
    void malformedIdsAreSkipped() {
        assertThat(indexCount().compute(List.of(A, "not-an-objectid"))).containsExactly(new EntityMetric<>(A, 3L));
        verify(indexSetService).findByIds(Set.of(A));
    }

    private IndexSetStatsDescriptor indexCount() {
        return new IndexSetStatsDescriptor.IndexCount(statsCreator, indexSetService, indexSetRegistry, TTL);
    }
}
