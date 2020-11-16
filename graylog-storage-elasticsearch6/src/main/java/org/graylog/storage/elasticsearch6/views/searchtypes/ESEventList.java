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
package org.graylog.storage.elasticsearch6.views.searchtypes;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ESEventList implements ESSearchTypeHandler<EventList> {
    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, EventList eventList,
                                    ESGeneratedQueryContext queryContext) {
        queryContext.searchSourceBuilder(eventList)
                .size(10000);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> extractResult(SearchResult result) {
        return result.getHits(Map.class, false).stream()
                .map(hit -> (Map<String, Object>) hit.source)
                .collect(Collectors.toList());
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, EventList searchType, SearchResult result,
                                             MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Set<String> effectiveStreams = searchType.streams().isEmpty()
                ? query.usedStreamIds()
                : searchType.streams();
        final List<EventSummary> eventSummaries = extractResult(result).stream()
                .map(EventSummary::parse)
                .filter(eventSummary -> effectiveStreams.containsAll(eventSummary.streams()))
                .collect(Collectors.toList());
        final EventList.Result.Builder resultBuilder = EventList.Result.builder()
                .events(eventSummaries)
                .id(searchType.id());
        searchType.name().ifPresent(resultBuilder::name);
        return resultBuilder
                .build();
    }
}
