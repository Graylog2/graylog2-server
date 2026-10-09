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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.indexer.indexset.index.IndexPattern;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.graylog2.utilities.FakeTicker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IndexSetStatsCreatorTest {
    private final Indices indices = mock(Indices.class);
    private final FakeTicker ticker = new FakeTicker(Duration.ZERO);
    private final IndexSetStatsCreator creator = new IndexSetStatsCreator(indices, ticker);

    @Test
    void getForIndexSetSumsOpenPrimariesAndCountsClosedIndices() throws Exception {
        final IndexSet indexSet = indexSet("a", "graylog");
        when(indices.getClosedIndices(indexSet)).thenReturn(Set.of("graylog_0"));
        when(indices.getIndexStats(indexSet)).thenReturn(new ObjectMapper().readTree("""
                {
                  "graylog_1": {"primaries": {"docs": {"count": 10}, "store": {"size_in_bytes": 100}}},
                  "graylog_2": {"primaries": {"docs": {"count": 5}, "store": {"size_in_bytes": 50}}}
                }
                """));

        assertThat(creator.getForIndexSet(indexSet)).isEqualTo(IndexSetStats.create(3, 15, 150));
    }

    @Test
    void getForIndexSetsGroupsBatchedStatsByIndexSet() throws Exception {
        when(indices.getClosedIndices(anyCollection())).thenReturn(Set.of("graylog_0", "other_3"));
        when(indices.getIndexStats(anyCollection())).thenReturn(new ObjectMapper().readTree("""
                {
                  "graylog_1": {"primaries": {"docs": {"count": 10}, "store": {"size_in_bytes": 100}}},
                  "graylog_2": {"primaries": {"docs": {"count": 5}, "store": {"size_in_bytes": 50}}},
                  "other_4": {"primaries": {"docs": {"count": 1}, "store": {"size_in_bytes": 1}}}
                }
                """));

        final Map<String, IndexSetStats> stats = creator.getForIndexSets(List.of(indexSet("a", "graylog"), indexSet("b", "other")));

        assertThat(stats).containsOnly(
                entry("a", IndexSetStats.create(3, 15, 150)),
                entry("b", IndexSetStats.create(2, 1, 1)));
        verify(indices).getIndexStats(anyCollection());
        verify(indices).getClosedIndices(anyCollection());
    }

    @Test
    void getForIndexSetsMakesNoCallsWithoutIndexSets() {
        assertThat(creator.getForIndexSets(List.of())).isEmpty();
        verifyNoInteractions(indices);
    }

    @Test
    void getForIndexSetsReusesStatsWithinTheMemoWindow() {
        when(indices.getIndexStats(anyCollection())).thenReturn(new ObjectMapper().createObjectNode());
        final IndexSet indexSet = indexSet("a", "graylog");

        creator.getForIndexSets(List.of(indexSet));
        creator.getForIndexSets(List.of(indexSet));
        verify(indices, times(1)).getIndexStats(anyCollection());

        ticker.advance(Duration.ofSeconds(11));
        creator.getForIndexSets(List.of(indexSet));
        verify(indices, times(2)).getIndexStats(anyCollection());
    }

    @Test
    void getForIndexSetsFetchesOnlyTheIndexSetsMissingFromTheMemo() throws Exception {
        when(indices.getIndexStats(anyCollection())).thenReturn(new ObjectMapper().readTree("""
                {
                  "graylog_1": {"primaries": {"docs": {"count": 10}, "store": {"size_in_bytes": 100}}},
                  "other_4": {"primaries": {"docs": {"count": 1}, "store": {"size_in_bytes": 1}}}
                }
                """));
        final IndexSet graylog = indexSet("a", "graylog");
        final IndexSet other = indexSet("b", "other");

        creator.getForIndexSets(List.of(graylog));
        final Map<String, IndexSetStats> stats = creator.getForIndexSets(List.of(graylog, other));

        verify(indices).getIndexStats(List.of("other_*"));
        assertThat(stats).containsOnly(
                entry("a", IndexSetStats.create(1, 10, 100)),
                entry("b", IndexSetStats.create(1, 1, 1)));
    }

    private static IndexSet indexSet(String id, String prefix) {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(id);
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getConfig()).thenReturn(config);
        when(indexSet.getIndexWildcard()).thenReturn(prefix + "_*");
        final IndexPattern pattern = new IndexPattern(Pattern.quote(prefix));
        when(indexSet.isManagedIndex(anyString()))
                .thenAnswer(invocation -> pattern.indexMatches(invocation.getArgument(0, String.class)));
        return indexSet;
    }
}
