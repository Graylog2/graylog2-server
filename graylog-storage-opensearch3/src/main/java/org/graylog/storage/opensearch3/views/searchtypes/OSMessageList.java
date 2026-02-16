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

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.plugins.views.search.LegacyDecoratorProcessor;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog.storage.opensearch3.views.MutableSearchRequestBuilder;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.joda.time.DateTime;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.SourceConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class OSMessageList implements OSSearchTypeHandler<MessageList> {
    private final LegacyDecoratorProcessor decoratorProcessor;
    private final ResultMessageFactory resultMessageFactory;
    private final boolean allowHighlighting;
    private final OSSerializationUtils serializationUtils;

    @Inject
    public OSMessageList(LegacyDecoratorProcessor decoratorProcessor,
                         ResultMessageFactory resultMessageFactory,
                         @Named("allow_highlighting") boolean allowHighlighting, OSSerializationUtils serializationUtils) {
        this.decoratorProcessor = decoratorProcessor;
        this.resultMessageFactory = resultMessageFactory;
        this.allowHighlighting = allowHighlighting;
        this.serializationUtils = serializationUtils;
    }

    private ResultMessage resultMessageFromSearchHit(Hit<JsonData> hit) {
        final Map<String, List<String>> highlights = hit.highlight();
        return resultMessageFactory.parseFromSource(hit.id(), hit.index(), serializationUtils.toMap(hit.source()), highlights);
    }

    @Override
    public void doGenerateQueryPart(Query query, MessageList messageList, OSGeneratedQueryContext queryContext) {

        final var searchSourceBuilder = queryContext.searchSourceBuilder(messageList)
                .size(messageList.limit())
                .from(messageList.offset());

        applyHighlightingIfActivated(searchSourceBuilder, query);

        final Set<String> effectiveStreamIds = query.effectiveStreams(messageList);

        if (!messageList.fields().isEmpty()) {
            searchSourceBuilder.source(SourceConfig.of(s -> s
                    .filter(f -> f.includes(messageList.fields())))
            );
        }

        List<Sort> sorts = firstNonNull(messageList.sort(), Collections.singletonList(Sort.create(Message.FIELD_TIMESTAMP, Sort.Order.DESC)));

        // Always add the gl2_second_sort_field alias, if sorting by timestamp is requested.
        // The alias points to gl2_message_id which contains a sequence nr that represents the order in which messages were received.
        // If messages have identical timestamps, we can still sort them correctly.
        final Optional<Sort> timeStampSort = findSort(sorts, Message.FIELD_TIMESTAMP);
        final Optional<Sort> msgIdSort = findSort(sorts, Message.FIELD_GL2_MESSAGE_ID);
        final Optional<Sort> secondSortField = findSort(sorts, Message.GL2_SECOND_SORT_FIELD);
        if (timeStampSort.isPresent() && msgIdSort.isEmpty() && secondSortField.isEmpty()) {
            sorts = new ArrayList<>(sorts);
            final Sort newMsgIdSort = Sort.create(Message.GL2_SECOND_SORT_FIELD, timeStampSort.get().order());
            sorts.add(sorts.indexOf(timeStampSort.get()) + 1, newMsgIdSort);
        }
        sorts.forEach(sort -> {

            FieldSort.Builder fieldSort = new FieldSort.Builder();
            fieldSort.field(sort.field()).order(toSortOrder(sort.order()));
            if (sort.field().equals(Message.GL2_SECOND_SORT_FIELD)) {
                fieldSort.unmappedType(FieldType.Keyword); // old indices might not have a mapping for gl2_second_sort_field
            } else {
                final Optional<String> fieldType = queryContext.fieldType(effectiveStreamIds, sort.field());
                fieldType.ifPresent(s -> fieldSort.unmappedType(toFieldType(s)));
            }
            searchSourceBuilder.sort(fieldSort.build()._toSortOptions());
        });
    }

    private static Optional<Sort> findSort(List<Sort> sorts, String search) {
        return sorts.stream().filter(s -> s.field().equals(search)).findFirst();
    }

    private SortOrder toSortOrder(Sort.Order sortOrder) {
        switch (sortOrder) {
            case ASC:
                return SortOrder.Asc;
            case DESC:
                return SortOrder.Desc;
            default:
                throw new IllegalStateException("Invalid sort order: " + sortOrder);
        }
    }

    private FieldType toFieldType(String fieldType) {
        return Arrays.stream(FieldType.values())
                .filter(f -> f.jsonValue().equals(fieldType) || f.name().equals(fieldType))
                .findFirst()
                .orElseThrow();
    }

    private void applyHighlightingIfActivated(MutableSearchRequestBuilder searchSourceBuilder, Query query) {
        if (!allowHighlighting) {
            return;
        }

        final QueryStringQuery highlightQuery = decoratedHighlightQuery(query);

        searchSourceBuilder
                .highlight(Highlight.of(h -> h
                        .requireFieldMatch(false)
                        .highlightQuery(highlightQuery.toQuery())
                        .fields("*", HighlightField.of(f -> f.matchedFields("*")))
                        .fragmentSize(0)
                        .numberOfFragments(0))
                );
    }

    private QueryStringQuery decoratedHighlightQuery(Query query) {
        final String queryString = query.query().queryString();
        return QueryStringQuery.of(b -> b.query(queryString));
    }

    @WithSpan
    @Override
    public SearchType.Result doExtractResult(Query query, MessageList searchType, MultiSearchItem<JsonData> result, OSGeneratedQueryContext queryContext) {
        final List<ResultMessageSummary> messages = result.hits().hits().stream()
                .map(this::resultMessageFromSearchHit)
                .map((resultMessage) -> ResultMessageSummary.create(resultMessage.highlightRanges, resultMessage.getMessage().getFields(), resultMessage.getIndex()))
                .collect(Collectors.toList());

        final String queryString = query.query().queryString();

        final DateTime from = query.effectiveTimeRange(searchType).getFrom();
        final DateTime to = query.effectiveTimeRange(searchType).getTo();

        final SearchResponse searchResponse = SearchResponse.create(
                queryString,
                queryString,
                Collections.emptySet(),
                messages,
                Collections.emptySet(),
                0,
                result.hits().total().value(),
                from,
                to
        );

        final SearchResponse decoratedSearchResponse = decoratorProcessor.decorateSearchResponse(searchResponse, searchType.decorators());

        final MessageList.Result.Builder resultBuilder = MessageList.Result.result(searchType.id())
                .messages(decoratedSearchResponse.messages())
                .effectiveTimerange(AbsoluteRange.create(from, to))
                .totalResults(decoratedSearchResponse.totalResults());
        return searchType.name().map(resultBuilder::name).orElse(resultBuilder).build();
    }
}
