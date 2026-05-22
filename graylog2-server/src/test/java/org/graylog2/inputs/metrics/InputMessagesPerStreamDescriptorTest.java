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
package org.graylog2.inputs.metrics;

import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.cache.MetricsCacheConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputMessagesPerStreamDescriptorTest {

    private MoreSearch moreSearch;
    private SearchUser searchUser;
    private InputMessagesPerStreamDescriptor descriptor;

    @BeforeEach
    void setUp() {
        moreSearch = mock(MoreSearch.class);
        searchUser = mock(SearchUser.class);
        descriptor = new InputMessagesPerStreamDescriptor(moreSearch, Duration.ofMinutes(5));
    }

    @Test
    void compute_queriesByInputIds() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Map.of(
                        "input1", Map.of("streamA", 100L, "streamB", 200L),
                        "input2", Map.of("streamC", 50L)
                ));

        final List<EntityMetric<Map<String, Long>>> result = descriptor.compute(List.of("input1", "input2"));

        verify(moreSearch).aggregateGroupedTerms(
                eq(FIELD_GL2_SOURCE_INPUT + ":input1 OR " + FIELD_GL2_SOURCE_INPUT + ":input2"),
                any(RelativeRange.class),
                eq(FIELD_GL2_SOURCE_INPUT), eq("streams"),
                eq(2), eq(MetricsCacheConfiguration.MAX_TERMS_SIZE));

        assertThat(result).extracting(EntityMetric::entityId).containsExactlyInAnyOrder("input1", "input2");
        assertThat(result).filteredOn(m -> m.entityId().equals("input1"))
                .first().extracting(EntityMetric::value)
                .isEqualTo(Map.of("streamA", 100L, "streamB", 200L));
        assertThat(result).filteredOn(m -> m.entityId().equals("input2"))
                .first().extracting(EntityMetric::value)
                .isEqualTo(Map.of("streamC", 50L));
    }

    @Test
    void compute_returnsEmptyMapForMissingEntities() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Map.of());

        final List<EntityMetric<Map<String, Long>>> result = descriptor.compute(List.of("input1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().entityId()).isEqualTo("input1");
        assertThat(result.getFirst().value()).isEmpty();
    }

    @Test
    void computeForUser_filtersToPermittedStreams() {
        when(searchUser.canReadStream("streamA")).thenReturn(true);
        when(searchUser.canReadStream("streamB")).thenReturn(false);
        when(searchUser.canReadStream("streamC")).thenReturn(true);

        final Map<String, Long> breakdown = Map.of("streamA", 100L, "streamB", 200L, "streamC", 50L);
        final Map<String, Long> filtered = descriptor.computeForUser(breakdown, searchUser);

        assertThat(filtered).containsOnly(
                Map.entry("streamA", 100L),
                Map.entry("streamC", 50L));
    }

    @Test
    void computeForUser_returnsEmptyWhenNoStreamsPermitted() {
        when(searchUser.canReadStream("streamA")).thenReturn(false);

        final Map<String, Long> filtered = descriptor.computeForUser(Map.of("streamA", 100L), searchUser);

        assertThat(filtered).isEmpty();
    }

    @Test
    void computeForUser_returnsEmptyForEmptyBreakdown() {
        final Map<String, Long> filtered = descriptor.computeForUser(Map.of(), searchUser);

        assertThat(filtered).isEmpty();
    }
}
