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
package org.graylog.plugins.views.search.elasticsearch;

import org.graylog.plugins.views.search.IndexRangeContainsOneOfStreams;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexLookupTest {

    private final TimeRange timeRangeWithNoIndexRanges = KeywordRange.create("42 years ago", TimeZone.getDefault().getID());
    private final TimeRange timeRangeWithMatchingIndexRange = KeywordRange.create("1 years ago", TimeZone.getDefault().getID());
    private final Set<String> streamIds = Set.of("s-1", "s-2");

    @Test
    void findsIndicesBelongingToStreamsInTimeRange() {
        final IndexRange indexRange1 = mockIndexRange("index1");
        final IndexRange indexRange2 = mockIndexRange("index2");
        final SortedSet<IndexRange> indexRanges = sortedSetOf(indexRange1, indexRange2);

        final IndexLookup sut = new IndexLookup(
                mockIndexRangeService(indexRanges, timeRangeWithMatchingIndexRange),
                mockStreamService(streamIds),
                mockIndexRangeContains(indexRange1));

        Set<String> result = sut.indexNamesForStreamsInTimeRange(streamIds, timeRangeWithMatchingIndexRange);
        assertThat(result).containsExactly(indexRange1.indexName());
    }

    @Test
    void returnsEmptySetForEmptyStreamIds() {
        final IndexLookup sut = new IndexLookup(mock(IndexRangeService.class), mockStreamService(Collections.emptySet()), mock(IndexRangeContainsOneOfStreams.class));
        Set<String> result = sut.indexNamesForStreamsInTimeRange(emptySet(), timeRangeWithNoIndexRanges);
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptySetIfNoIndicesFound() {
        final IndexLookup sut = new IndexLookup(mock(IndexRangeService.class), mockStreamService(streamIds), mock(IndexRangeContainsOneOfStreams.class));
        Set<String> result = sut.indexNamesForStreamsInTimeRange(streamIds, timeRangeWithNoIndexRanges);
        assertThat(result).isEmpty();
    }

    private IndexRangeService mockIndexRangeService(SortedSet<IndexRange> indexRanges, TimeRange timeRangeWithMatchingIndexRange) {
        final IndexRangeService indexRangeService = mock(IndexRangeService.class);
        when(indexRangeService.find(timeRangeWithMatchingIndexRange.getFrom(), timeRangeWithMatchingIndexRange.getTo())).thenReturn(indexRanges);
        return indexRangeService;
    }

    private static IndexRangeContainsOneOfStreams mockIndexRangeContains(IndexRange matchingIndexRange) {
        IndexRangeContainsOneOfStreams indexRangeContainsOneOfStreams = mock(IndexRangeContainsOneOfStreams.class);
        doReturn(true).when(indexRangeContainsOneOfStreams).test(eq(matchingIndexRange), any());
        return indexRangeContainsOneOfStreams;
    }

    private StreamService mockStreamService(Set<String> streamIds) {
        final Set<Stream> streams = streamIds.stream()
                .map(id -> mock(Stream.class))
                .collect(Collectors.toSet());

        final StreamService streamService = mock(StreamService.class);
        if (!streamIds.isEmpty()) {
            when(streamService.loadByIds(streamIds)).thenReturn(streams);
        }
        return streamService;
    }

    private IndexRange mockIndexRange(final String name) {
        final IndexRange indexRange1 = mock(IndexRange.class);
        when(indexRange1.indexName()).thenReturn(name);
        return indexRange1;
    }

    SortedSet<IndexRange> sortedSetOf(IndexRange... indexRanges) {
        final Comparator<IndexRange> indexRangeComparator = Comparator.comparing(IndexRange::indexName);
        final TreeSet<IndexRange> indexRangeSets = new TreeSet<>(indexRangeComparator);
        indexRangeSets.addAll(Arrays.asList(indexRanges));
        return indexRangeSets;
    }
}
