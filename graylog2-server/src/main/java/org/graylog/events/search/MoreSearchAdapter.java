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
package org.graylog.events.search;

import org.graylog.events.processor.EventProcessorException;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.NumberRange;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.resources.entities.Slice;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public interface MoreSearchAdapter {
    default MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices, Sorting sorting,
                                          int page, int perPage, Set<String> eventStreams, String filterString, SourceStreamFilter sourceStreamFilter) {
        return eventSearch(queryString, timerange, affectedIndices, sorting, page, perPage, eventStreams, filterString, sourceStreamFilter, Map.of());
    }
    MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices, Sorting sorting,
                                  int page, int perPage, Set<String> eventStreams, String filterString, SourceStreamFilter sourceStreamFilter,
                                  Map<String, Set<String>> extraFilters);

    MoreSearch.Histogram eventHistogram(String queryString, AbsoluteRange timerange, Set<String> affectedIndices,
                                        Set<String> eventStreams, String filterString, SourceStreamFilter sourceStreamFilter,
                                        ZoneId timeZone, Map<String, Set<String>> extraFilters);

    interface ScrollEventsCallback {
        void accept(List<ResultMessage> results, AtomicBoolean requestContinue) throws EventProcessorException;
    }

    void scrollEvents(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams,
                      List<UsedSearchFilter> filters, int batchSize, ScrollEventsCallback resultCallback) throws EventProcessorException;

    List<Slice> aggregateSlicesForColumn(String queryString, TimeRange timerange, Set<String> affectedIndices,
                                Set<String> eventStreams, String filterString, SourceStreamFilter sourceStreamFilter,
                                Map<String, Set<String>> extraFilters, String slicingColumn, Map<String, Object> meta, int maxBuckets);

    List<Slice> aggregateSlicesForRangeQuery(String queryString, TimeRange timerange, Set<String> affectedIndices,
                                           Set<String> eventStreams, String filterString, SourceStreamFilter sourceStreamFilter,
                                           Map<String, Set<String>> extraFilters, String slicingColumn, Map<String, Object> meta, List<NumberRange> ranges);

    // The aggregation methods below do not perform stream-based permission filtering.
    // Callers are responsible for their own permission checks (e.g. via computeForUser in descriptors).

    /**
     * Groups documents by {@code groupByField}, then performs a terms sub-aggregation on {@code termsField}
     * within each bucket. Returns a map of group key to sub-term counts.
     * <p>
     * When {@code groupByField} is multi-valued (e.g. {@code streams}), pass the requested IDs as
     * {@code includeTerms} to prevent unrequested co-occurring values from consuming bucket slots.
     * Pass an empty collection to include all terms.
     *
     * @return map of group key → (sub-term key → doc count)
     */
    Map<String, Map<String, Long>> aggregateGroupedTerms(String queryString, TimeRange timerange, Set<String> affectedIndices,
                                                         String groupByField, String termsField,
                                                         int maxBuckets, int maxSubBuckets,
                                                         Collection<String> includeTerms);

    /**
     * Performs a terms aggregation on {@code termsField} and returns the doc count per term.
     * <p>
     * Pass the requested IDs as {@code includeTerms} when aggregating on multi-valued fields.
     * Pass an empty collection to include all terms.
     *
     * @return map of term value → doc count
     */
    Map<String, Long> aggregateTerms(String queryString, TimeRange timerange, Set<String> affectedIndices,
                                     String termsField, int maxBuckets,
                                     Collection<String> includeTerms);

    enum AggregationType { AVG, MAX }

    /**
     * Groups documents by {@code groupByField}, then computes a metric (avg or max) of {@code metricField}
     * within each bucket.
     * <p>
     * Pass the requested IDs as {@code includeTerms} when aggregating on multi-valued fields.
     * Pass an empty collection to include all terms.
     *
     * @return map of group key → metric value
     */
    Map<String, Double> aggregateGroupedMetric(String queryString, TimeRange timerange, Set<String> affectedIndices,
                                               String groupByField, AggregationType aggregationType, String metricField,
                                               int maxBuckets, Collection<String> includeTerms);

    default ChunkCommand buildScrollCommand(String queryString, TimeRange timeRange, Set<String> affectedIndices, List<UsedSearchFilter> filters, Set<String> streams, int batchSize) {
        ChunkCommand.Builder commandBuilder = ChunkCommand.builder()
                .query(queryString)
                .range(timeRange)
                .indices(affectedIndices)
                .filters(filters == null ? Collections.emptyList() : filters)
                .batchSize(batchSize)
                // For correlation need the oldest messages to come in first
                .sorting(new Sorting(Message.FIELD_TIMESTAMP, Sorting.Direction.ASC));

        if (!streams.isEmpty()) {
            commandBuilder = commandBuilder.streams(streams);
        }

        return commandBuilder
                .build();
    }

    static boolean isRangeValue(String value) {
        return value.startsWith("<=") || value.startsWith(">=") || value.startsWith("<") || value.startsWith(">");
    }
}
