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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class IndexLookupTest {

    @Mock
    private IndexRangeService indexRangeService;
    @Mock
    private StreamService streamService;
    @Mock
    private IndexRangeContainsOneOfStreams indexRangeContainsOneOfStreams;
    private IndexLookup sut;

    private final TimeRange timeRangeWithNoIndexRanges = KeywordRange.create("42 years ago", TimeZone.getDefault().getID());
    private final TimeRange timeRangeWithMatchingIndexRange = KeywordRange.create("1 years ago", TimeZone.getDefault().getID());
    private final Set<String> streamIds = Set.of("s-1", "s-2");
    private Set<Stream> streams;

    @BeforeEach
    void setUp() {
        sut = new IndexLookup(indexRangeService, streamService, indexRangeContainsOneOfStreams);

        streams = streamIds.stream()
                .map(id -> mock(Stream.class))
                .collect(Collectors.toSet());
    }

    @Test
    void findsIndicesBelongingToStreamsInTimeRange() {
        when(streamService.loadByIds(streamIds)).thenReturn(streams);

        List<IndexRange> indexRanges = mockSomeIndexRanges();
        IndexRange matchingIndexRange = indexRanges.get(0);
        doReturn(true).when(indexRangeContainsOneOfStreams).test(eq(matchingIndexRange), any());
        Set<String> result = sut.indexNamesForStreamsInTimeRange(streamIds, timeRangeWithMatchingIndexRange);

        assertThat(result).containsExactly(matchingIndexRange.indexName());
    }

    @Test
    void returnsEmptySetForEmptyStreamIds() {
        Set<String> result = sut.indexNamesForStreamsInTimeRange(emptySet(), timeRangeWithNoIndexRanges);
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptySetIfNoIndicesFound() {
        when(streamService.loadByIds(streamIds)).thenReturn(streams);
        Set<String> result = sut.indexNamesForStreamsInTimeRange(streamIds, timeRangeWithNoIndexRanges);
        assertThat(result).isEmpty();
    }

    private List<IndexRange> mockSomeIndexRanges() {
        final IndexRange indexRange1 = mockIndexRange("index1");
        final IndexRange indexRange2 = mockIndexRange("index2");

        final SortedSet<IndexRange> indexRanges = sortedSetOf(indexRange1, indexRange2);
        when(indexRangeService.find(timeRangeWithMatchingIndexRange.getFrom(), timeRangeWithMatchingIndexRange.getTo())).thenReturn(indexRanges);

        return new ArrayList<>(indexRanges);
    }

    private IndexRange mockIndexRange(final String name) {
        final IndexRange indexRange1 = mock(IndexRange.class);
        when(indexRange1.indexName()).thenReturn(name);
        return indexRange1;
    }

    SortedSet<IndexRange> sortedSetOf(IndexRange... indexRanges) {
        final Comparator<IndexRange> indexRangeComparator = Comparator.comparing(IndexRange::indexName);

        final TreeSet<IndexRange> indexRangeSets = new TreeSet<>(indexRangeComparator);

        indexRangeSets.addAll(java.util.Arrays.asList(indexRanges));

        return indexRangeSets;
    }
}
