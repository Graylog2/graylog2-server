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
import org.graylog.events.search.SourceStreamFilter;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Message.FIELD_STREAMS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamMessageCountDescriptorTest {

    private MoreSearch moreSearch;
    private StreamMessageCountDescriptor descriptor;

    @BeforeEach
    void setUp() {
        moreSearch = mock(MoreSearch.class);
        descriptor = new StreamMessageCountDescriptor(moreSearch, Duration.ofMinutes(5));
    }

    @Test
    void compute_queriesByStreamIds() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class), any(SourceStreamFilter.class),
                anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Map.of(
                        "stream-1", Map.of("stream-1", 500L),
                        "stream-2", Map.of("stream-2", 200L)
                ));

        final List<EntityMetric<Long>> result = descriptor.compute(List.of("stream-1", "stream-2"));

        verify(moreSearch).aggregateGroupedTerms(
                eq(FIELD_STREAMS + ":stream-1 OR " + FIELD_STREAMS + ":stream-2"),
                any(RelativeRange.class),
                eq(SourceStreamFilter.allAllowed()),
                eq(FIELD_STREAMS), eq(FIELD_STREAMS),
                eq(2), eq(1));

        assertThat(result).extracting(EntityMetric::entityId).containsExactlyInAnyOrder("stream-1", "stream-2");
        assertThat(result).filteredOn(m -> m.entityId().equals("stream-1"))
                .first().extracting(EntityMetric::value).isEqualTo(500L);
        assertThat(result).filteredOn(m -> m.entityId().equals("stream-2"))
                .first().extracting(EntityMetric::value).isEqualTo(200L);
    }

    @Test
    void compute_returnsZeroForMissingStreams() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class), any(SourceStreamFilter.class),
                anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Map.of());

        final List<EntityMetric<Long>> result = descriptor.compute(List.of("stream-1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isEqualTo(0L);
    }

    @Test
    void computeForUser_returnsValueAsIs() {
        assertThat(descriptor.computeForUser(42L, null)).isEqualTo(42L);
    }
}
