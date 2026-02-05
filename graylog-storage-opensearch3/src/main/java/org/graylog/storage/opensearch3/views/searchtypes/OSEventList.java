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
package org.graylog.storage.opensearch3.views.searchtypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import org.graylog.events.event.EventDto;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.events.CommonEventSummary;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OSEventList implements EventListStrategy {
    private final ObjectMapper objectMapper;
    private final OSSerializationUtils serializationUtils;

    @Inject
    public OSEventList(ObjectMapper objectMapper, OSSerializationUtils serializationUtils) {
        this.objectMapper = objectMapper;
        this.serializationUtils = serializationUtils;
    }

    @Override
    public void doGenerateQueryPart(Query query, EventList eventList,
                                    OSGeneratedQueryContext queryContext) {
        final Set<String> effectiveStreams = eventList.streams().isEmpty()
                ? query.usedStreamIds()
                : eventList.streams();

        final var searchSourceBuilder = queryContext.searchSourceBuilder(eventList);
        final FieldSort.Builder sortConfig = sortConfig(eventList);
        searchSourceBuilder.sort(sortConfig.build()._toSortOptions());
        final var originalQuery = searchSourceBuilder.build().query();
        if (!effectiveStreams.isEmpty() && originalQuery.isBool()) {
            searchSourceBuilder.query(
                    originalQuery.bool().toBuilder().must(TermsQuery.of(t -> t
                                            .field(EventDto.FIELD_SOURCE_STREAMS)
                                            .terms(ts -> ts.value(effectiveStreams.stream().map(FieldValue::of).toList()))
                                    )
                                    .toQuery()
                    ).build().toQuery());
        }
        if (!eventList.attributes().isEmpty() && originalQuery.isBool()) {
            final var filterQueries = eventList.attributes().stream()
                    .filter(attribute -> EventList.KNOWN_ATTRIBUTES.contains(attribute.field()))
                    .flatMap(attribute -> attribute.toQueryStrings().stream())
                    .toList();

            filterQueries.forEach(filterQuery ->
                    searchSourceBuilder.query(
                            searchSourceBuilder.build().query().bool().toBuilder()
                                    .filter(QueryStringQuery.of(q -> q.query(filterQuery)).toQuery())
                                    .build().toQuery()
                    ));
        }

        eventList.page().ifPresentOrElse(page -> {
            final int pageSize = eventList.perPage().orElse(EventList.DEFAULT_PAGE_SIZE);
            searchSourceBuilder.size(pageSize);
            searchSourceBuilder.from((page - 1) * pageSize);
        }, () -> searchSourceBuilder.size(10000));
    }

    private SortOrder toSortOrder(EventList.Direction direction) {
        return switch (direction) {
            case ASC -> SortOrder.Asc;
            case DESC -> SortOrder.Desc;
        };
    }

    protected FieldSort.Builder sortConfig(EventList eventList) {
        final var sortConfig = eventList.sort()
                .filter(sort -> EventList.KNOWN_ATTRIBUTES.contains(sort.field()))
                .orElse(EventList.DEFAULT_SORT);
        return new FieldSort.Builder()
                .field(sortConfig.field())
                .order(toSortOrder(sortConfig.direction()));
    }

    protected List<Map<String, Object>> extractResult(MultiSearchItem<JsonData> result) {
        return result.hits().hits().stream()
                .map(r -> {
                    try {
                        return serializationUtils.toMap(r);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    @WithSpan
    @Override
    public SearchType.Result doExtractResult(Query query, EventList searchType, MultiSearchItem<JsonData> result,
                                             OSGeneratedQueryContext queryContext) {
        final List<CommonEventSummary> eventSummaries = extractResult(result).stream()
                .map(rawEvent -> objectMapper.convertValue(rawEvent, EventDto.class))
                .map(EventSummary::parse)
                .collect(Collectors.toList());
        final EventList.Result.Builder resultBuilder = EventList.Result.builder()
                .events(eventSummaries)
                .id(searchType.id())
                .totalResults(result.hits().total().value());
        searchType.name().ifPresent(resultBuilder::name);
        return resultBuilder.build();
    }
}
