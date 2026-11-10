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
import static org.mockito.ArgumentMatchers.anyCollection;
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
        when(moreSearch.aggregateTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyInt(), anyCollection()))
                .thenReturn(Map.of("stream1", 500L, "stream2", 200L));

        final List<EntityMetric<Long>> result = descriptor.compute(List.of("stream1", "stream2"));

        verify(moreSearch).aggregateTerms(
                eq(FIELD_STREAMS + ":stream1 OR " + FIELD_STREAMS + ":stream2"),
                any(RelativeRange.class),
                eq(FIELD_STREAMS), eq(2),
                eq(List.of("stream1", "stream2")));

        assertThat(result).extracting(EntityMetric::entityId).containsExactlyInAnyOrder("stream1", "stream2");
        assertThat(result).filteredOn(m -> m.entityId().equals("stream1"))
                .first().extracting(EntityMetric::value).isEqualTo(500L);
        assertThat(result).filteredOn(m -> m.entityId().equals("stream2"))
                .first().extracting(EntityMetric::value).isEqualTo(200L);
    }

    @Test
    void compute_returnsZeroForMissingStreams() {
        when(moreSearch.aggregateTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyInt(), anyCollection()))
                .thenReturn(Map.of());

        final List<EntityMetric<Long>> result = descriptor.compute(List.of("stream1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isEqualTo(0L);
    }

    @Test
    void computeForUser_returnsValueAsIs() {
        assertThat(descriptor.computeForUser(42L, null)).isEqualTo(42L);
    }
}
