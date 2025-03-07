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
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public interface MoreSearchAdapter {
    default MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices, Sorting sorting,
                                          int page, int perPage, Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams) {
        return eventSearch(queryString, timerange, affectedIndices, sorting, page, perPage, eventStreams, filterString, forbiddenSourceStreams, Map.of());
    }
    MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices, Sorting sorting,
                                  int page, int perPage, Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams,
                                  Map<String, Set<String>> extraFilters);

    MoreSearch.Histogram eventHistogram(int buckets, String queryString, AbsoluteRange timerange, Set<String> affectedIndices,
                                        Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams,
                                        ZoneId timeZone, Map<String, Set<String>> extraFilters);

    interface ScrollEventsCallback {
        void accept(List<ResultMessage> results, AtomicBoolean requestContinue) throws EventProcessorException;
    }

    void scrollEvents(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams,
                      List<UsedSearchFilter> filters, int batchSize, ScrollEventsCallback resultCallback) throws EventProcessorException;
}
