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

import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexLookupTest {

    private IndexRangeService indexRangeService;
    private StreamService streamService;
    private IndexLookup sut;

    @BeforeEach
    void setUp() {
        indexRangeService = mock(IndexRangeService.class);
        streamService = mock(StreamService.class);
        sut = new IndexLookup(indexRangeService, streamService);
    }

    @Test
    void findsIndicesBelongingToStreamsInTimeRange() {

        Set<String> streamIds = mockStreams("s-1", "s-2");

        List<IndexRange> indexRanges = mockSomeIndexRanges();

        IndexRange matchingIndexRange = indexRanges.get(0);

        sut.indexRangeContainsOneOfStreams = (i, s) -> i.equals(matchingIndexRange);

        Set<String> result = sut.indexNamesForStreamsInTimeRange(streamIds, someTimeRange());

        assertThat(result).containsExactly(matchingIndexRange.indexName());
    }

    @Test
    void returnsEmptySetForEmptyStreamIds() {
        Set<String> result = sut.indexNamesForStreamsInTimeRange(emptySet(), someTimeRange());

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptySetIfNoIndicesFound() {
        Set<String> streamIds = mockStreams("1", "2");

        sut.indexRangeContainsOneOfStreams = (i, s) -> true;

        Set<String> result = sut.indexNamesForStreamsInTimeRange(streamIds, someTimeRange());

        assertThat(result).isEmpty();
    }

    private RelativeRange someTimeRange() {
        try {
            return RelativeRange.create(1);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }

    private List<IndexRange> mockSomeIndexRanges() {
        final IndexRange indexRange1 = mockIndexRange("index1");
        final IndexRange indexRange2 = mockIndexRange("index2");

        final SortedSet<IndexRange> indexRanges = sortedSetOf(indexRange1, indexRange2);
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indexRanges);

        return new ArrayList<>(indexRanges);
    }

    private Set<String> mockStreams(String... ids) {
        Set<Stream> streams = Arrays.stream(ids).map(this::mockStream).collect(Collectors.toSet());

        when(streamService.loadByIds(any())).thenReturn(streams);

        return streams.stream().map(Persisted::getId).collect(Collectors.toSet());
    }

    private IndexRange mockIndexRange(String name) {
        final IndexRange indexRange1 = mock(IndexRange.class);
        when(indexRange1.indexName()).thenReturn(name);
        return indexRange1;
    }

    private Stream mockStream(String id) {
        final Stream s = mock(Stream.class, RETURNS_DEEP_STUBS);
        when(s.getId()).thenReturn(id);
        return s;
    }

    SortedSet<IndexRange> sortedSetOf(IndexRange... indexRanges) {
        final Comparator<IndexRange> indexRangeComparator = Comparator.comparing(IndexRange::indexName);

        final TreeSet<IndexRange> indexRangeSets = new TreeSet<>(indexRangeComparator);

        indexRangeSets.addAll(java.util.Arrays.asList(indexRanges));

        return indexRangeSets;
    }
}
