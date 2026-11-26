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

import org.graylog2.indexer.IndexFailureCounter;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexSetIndexingFailuresDescriptorTest {
    private static final String A = "000000000000000000000001";
    private static final String B = "000000000000000000000002";
    private final IndexFailureCounter indexFailureCounter = mock(IndexFailureCounter.class);
    private final IndexSetService indexSetService = mock(IndexSetService.class);
    private final IndexSetRegistry indexSetRegistry = mock(IndexSetRegistry.class);
    private final IndexSetIndexingFailuresDescriptor descriptor = new IndexSetIndexingFailuresDescriptor(
            indexFailureCounter, indexSetService, indexSetRegistry, Duration.ofMinutes(1));

    @Test
    void sumsTheFailuresOfEachIndexSetsIndicesWithinTheWindow() {
        final IndexSet graylog = managing(IndexSetMocks.indexSet(A, "graylog", true), "graylog_1", "graylog_2");
        final IndexSet archive = IndexSetMocks.indexSet(B, "archive", true);
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry, graylog, archive);
        when(indexFailureCounter.countByIndexSince(any()))
                .thenReturn(Map.of("graylog_1", 2L, "graylog_2", 3L, "other_1", 7L, "UNKNOWN", 1L));

        final DateTime before = Tools.nowUTC();
        assertThat(descriptor.compute(List.of(A, B))).containsExactlyInAnyOrder(
                new EntityMetric<>(A, 5L),
                new EntityMetric<>(B, 0L));
        final DateTime after = Tools.nowUTC();

        final ArgumentCaptor<DateTime> since = ArgumentCaptor.forClass(DateTime.class);
        verify(indexFailureCounter).countByIndexSince(since.capture());
        assertThat(since.getValue()).isBetween(before.minusMinutes(10), after.minusMinutes(10));
    }

    @Test
    void reportsNoValueWhenTheCountFails() {
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry, IndexSetMocks.indexSet(A, "graylog", true));
        when(indexFailureCounter.countByIndexSince(any())).thenThrow(new IllegalStateException("Connection refused"));

        assertThat(descriptor.compute(List.of(A))).isEmpty();
    }

    private static IndexSet managing(IndexSet indexSet, String... indices) {
        for (String index : indices) {
            when(indexSet.isManagedIndex(index)).thenReturn(true);
        }
        return indexSet;
    }
}
