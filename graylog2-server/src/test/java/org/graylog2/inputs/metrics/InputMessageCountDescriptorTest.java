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
import org.graylog.events.search.SourceStreamFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetric;
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

class InputMessageCountDescriptorTest {

    private MoreSearch moreSearch;
    private SearchUser searchUser;
    private InputMessageCountDescriptor descriptor;

    @BeforeEach
    void setUp() {
        moreSearch = mock(MoreSearch.class);
        searchUser = mock(SearchUser.class);
        descriptor = new InputMessageCountDescriptor(moreSearch, Duration.ofMinutes(5));
    }

    @Test
    void compute_queriesByInputIds() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class), any(SourceStreamFilter.class),
                anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Map.of(
                        "input-1", Map.of("stream-a", 100L, "stream-b", 200L),
                        "input-2", Map.of("stream-c", 50L)
                ));

        final List<EntityMetric<Map<String, Long>>> result = descriptor.compute(List.of("input-1", "input-2"));

        verify(moreSearch).aggregateGroupedTerms(
                eq(FIELD_GL2_SOURCE_INPUT + ":input-1 OR " + FIELD_GL2_SOURCE_INPUT + ":input-2"),
                any(RelativeRange.class),
                eq(SourceStreamFilter.allAllowed()),
                eq(FIELD_GL2_SOURCE_INPUT), eq("streams"),
                eq(2), eq(Integer.MAX_VALUE));

        assertThat(result).extracting(EntityMetric::entityId).containsExactlyInAnyOrder("input-1", "input-2");
        assertThat(result).filteredOn(m -> m.entityId().equals("input-1"))
                .first().extracting(EntityMetric::value)
                .isEqualTo(Map.of("stream-a", 100L, "stream-b", 200L));
        assertThat(result).filteredOn(m -> m.entityId().equals("input-2"))
                .first().extracting(EntityMetric::value)
                .isEqualTo(Map.of("stream-c", 50L));
    }

    @Test
    void compute_returnsEmptyMapForMissingEntities() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class), any(SourceStreamFilter.class),
                anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Map.of());

        final List<EntityMetric<Map<String, Long>>> result = descriptor.compute(List.of("input-1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().entityId()).isEqualTo("input-1");
        assertThat(result.getFirst().value()).isEmpty();
    }

    @Test
    void computeForUser_sumsOnlyPermittedStreams() {
        when(searchUser.canReadStream("stream-a")).thenReturn(true);
        when(searchUser.canReadStream("stream-b")).thenReturn(false);
        when(searchUser.canReadStream("stream-c")).thenReturn(true);

        final Map<String, Long> breakdown = Map.of("stream-a", 100L, "stream-b", 200L, "stream-c", 50L);
        final Long filtered = descriptor.computeForUser(breakdown, searchUser);

        assertThat(filtered).isEqualTo(150L);
    }

    @Test
    void computeForUser_returnsZeroWhenNoStreamsPermitted() {
        when(searchUser.canReadStream("stream-a")).thenReturn(false);

        final Long filtered = descriptor.computeForUser(Map.of("stream-a", 100L), searchUser);

        assertThat(filtered).isEqualTo(0L);
    }

    @Test
    void computeForUser_returnsZeroForEmptyBreakdown() {
        final Long filtered = descriptor.computeForUser(Map.of(), searchUser);

        assertThat(filtered).isEqualTo(0L);
    }
}
