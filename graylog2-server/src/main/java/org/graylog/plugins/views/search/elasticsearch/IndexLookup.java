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

import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.IndexRangeContainsOneOfStreams;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;

import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class IndexLookup {

    private final IndexRangeService indexRangeService;
    private final StreamService streamService;
    private final IndexRangeContainsOneOfStreams indexRangeContainsOneOfStreams;

    @Inject
    public IndexLookup(final IndexRangeService indexRangeService,
                       final StreamService streamService) {
        this.indexRangeService = indexRangeService;
        this.streamService = streamService;
        this.indexRangeContainsOneOfStreams = new IndexRangeContainsOneOfStreams();
    }

    IndexLookup(final IndexRangeService indexRangeService,
                final StreamService streamService,
                final IndexRangeContainsOneOfStreams indexRangeContainsOneOfStreams) {
        this.indexRangeService = indexRangeService;
        this.streamService = streamService;
        this.indexRangeContainsOneOfStreams = indexRangeContainsOneOfStreams;
    }

    public Set<String> indexNamesForStreamsInTimeRange(final Set<String> streamIds,
                                                       final TimeRange timeRange) {

        Set<String> dataStreamIndices = streamIds.stream()
                .filter(s -> s.startsWith(Stream.DATASTREAM_PREFIX))
                .map(s -> s.substring(Stream.DATASTREAM_PREFIX.length()))
                .collect(Collectors.toSet());

        final Set<String> candidateIndices = indexRangesForStreamsInTimeRange(streamIds, timeRange).stream().map(IndexRange::indexName).collect(Collectors.toSet());

        return Sets.union(dataStreamIndices, candidateIndices);
    }

    public Set<IndexRange> indexRangesForStreamsInTimeRange(final Set<String> streamIds,
                                                            final TimeRange timeRange) {

        if (streamIds.isEmpty()) {
            return Set.of();
        }
        var nonDataStreamIds = streamIds.stream().filter(s -> !s.startsWith(Stream.DATASTREAM_PREFIX)).collect(Collectors.toSet());

        final Set<Stream> usedStreams = streamService.loadByIds(nonDataStreamIds);
        final SortedSet<IndexRange> candidateIndices = indexRangeService.find(timeRange.getFrom(), timeRange.getTo());

        return candidateIndices.stream()
                .filter(i -> indexRangeContainsOneOfStreams.test(i, usedStreams))
                .collect(Collectors.toSet());
    }
}
