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
package org.graylog2.streams.metrics;

import org.graylog.events.search.MoreSearch;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Message.FIELD_GL2_PROCESSING_DURATION_MS;
import static org.graylog2.plugin.Message.FIELD_STREAMS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamMaxProcessingTimeDescriptorTest {

    private MoreSearch moreSearch;
    private StreamMaxProcessingTimeDescriptor descriptor;

    @BeforeEach
    void setUp() {
        moreSearch = mock(MoreSearch.class);
        descriptor = new StreamMaxProcessingTimeDescriptor(moreSearch, Duration.ofMinutes(1));
    }

    @Test
    void compute_queriesMaxProcessingTime() {
        when(moreSearch.aggregateGroupedMetric(
                anyString(), any(RelativeRange.class),
                anyString(), any(MoreSearchAdapter.AggregationType.class), anyString(), anyInt(),
                anyCollection()))
                .thenReturn(Map.of("stream1", 150.0));

        final List<EntityMetric<Double>> result = descriptor.compute(List.of("stream1"));

        verify(moreSearch).aggregateGroupedMetric(
                eq(FIELD_STREAMS + ":stream1"),
                any(RelativeRange.class),
                eq(FIELD_STREAMS), eq(MoreSearchAdapter.AggregationType.MAX),
                eq(FIELD_GL2_PROCESSING_DURATION_MS),
                eq(1),
                eq(List.of("stream1")));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isEqualTo(150.0);
    }

    @Test
    void compute_returnsZeroForMissingStreams() {
        when(moreSearch.aggregateGroupedMetric(
                anyString(), any(RelativeRange.class),
                anyString(), any(MoreSearchAdapter.AggregationType.class), anyString(), anyInt(),
                anyCollection()))
                .thenReturn(Map.of());

        final List<EntityMetric<Double>> result = descriptor.compute(List.of("stream1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isEqualTo(0.0);
    }

    @Test
    void computeForUser_returnsValueAsIs() {
        assertThat(descriptor.computeForUser(150.0, null)).isEqualTo(150.0);
    }
}
