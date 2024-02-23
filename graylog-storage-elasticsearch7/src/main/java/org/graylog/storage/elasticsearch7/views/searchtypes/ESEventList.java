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
package org.graylog.storage.elasticsearch7.views.searchtypes;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortOrder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog2.plugin.Message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ESEventList implements ESSearchTypeHandler<EventList> {
    @Override
    public void doGenerateQueryPart(Query query, EventList eventList,
                                    ESGeneratedQueryContext queryContext) {
        final var searchSourceBuilder = queryContext.searchSourceBuilder(eventList);
        searchSourceBuilder.sort(Message.FIELD_TIMESTAMP, SortOrder.DESC);
        final var queryBuilder = searchSourceBuilder.query();
        if (!eventList.attributes().isEmpty() && queryBuilder instanceof BoolQueryBuilder boolQueryBuilder) {
            final var filterQuery = eventList.attributes().stream()
                    .flatMap(attribute -> attribute.toQueryStrings().stream())
                    .collect(Collectors.joining(" AND "));
            boolQueryBuilder.filter(QueryBuilders.queryStringQuery(filterQuery));
        }

        searchSourceBuilder.size(10000);
        eventList.page().ifPresent(page -> {
            final var pageSize = eventList.perPage().orElse(EventList.DEFAULT_PAGE_SIZE);
            searchSourceBuilder.size(pageSize);
            searchSourceBuilder.from(page * pageSize);
        });
    }

    protected List<Map<String, Object>> extractResult(SearchResponse result) {
        return StreamSupport.stream(result.getHits().spliterator(), false)
                .map(SearchHit::getSourceAsMap)
                .collect(Collectors.toList());
    }

    @WithSpan
    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, EventList searchType, SearchResponse result,
                                             Aggregations aggregations, ESGeneratedQueryContext queryContext) {
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
