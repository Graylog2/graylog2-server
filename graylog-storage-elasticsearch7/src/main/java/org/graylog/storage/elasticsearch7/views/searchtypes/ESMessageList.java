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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.name.Named;
import org.graylog.plugins.views.search.LegacyDecoratorProcessor;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.text.Text;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.FieldSortBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortOrder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;

public class ESMessageList implements ESSearchTypeHandler<MessageList> {
    private final QueryStringDecorators esQueryDecorators;
    private final LegacyDecoratorProcessor decoratorProcessor;
    private final boolean allowHighlighting;

    @Inject
    public ESMessageList(QueryStringDecorators esQueryDecorators,
                         LegacyDecoratorProcessor decoratorProcessor,
                         @Named("allow_highlighting") boolean allowHighlighting) {
        this.esQueryDecorators = esQueryDecorators;
        this.decoratorProcessor = decoratorProcessor;
        this.allowHighlighting = allowHighlighting;
    }

    @VisibleForTesting
    public ESMessageList(QueryStringDecorators esQueryDecorators) {
        this(esQueryDecorators, new LegacyDecoratorProcessor.Fake(), false);
    }

    private static ResultMessage resultMessageFromSearchHit(SearchHit hit) {
        final Map<String, List<String>> highlights = hit.getHighlightFields().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, ESMessageList::highlightsFromFragments));
        return ResultMessage.parseFromSource(hit.getId(), hit.getIndex(), hit.getSourceAsMap(), highlights);
    }

    private static List<String> highlightsFromFragments(Map.Entry<String, HighlightField> entry) {
        return Arrays.stream(entry.getValue().fragments())
                .map(Text::toString)
                .collect(Collectors.toList());
    }

    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, MessageList messageList, ESGeneratedQueryContext queryContext) {

        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(messageList)
                .size(messageList.limit())
                .from(messageList.offset());

        applyHighlightingIfActivated(searchSourceBuilder, job, query);

        final Set<String> effectiveStreamIds = messageList.effectiveStreams().isEmpty()
                ? query.usedStreamIds()
                : messageList.effectiveStreams();

        final List<Sort> sorts = firstNonNull(messageList.sort(), Collections.singletonList(Sort.create(Message.FIELD_TIMESTAMP, Sort.Order.DESC)));
        sorts.forEach(sort -> {
            final FieldSortBuilder fieldSort = SortBuilders.fieldSort(sort.field())
                    .order(toSortOrder(sort.order()));
            final Optional<String> fieldType = queryContext.fieldType(effectiveStreamIds, sort.field());
            searchSourceBuilder.sort(fieldType.map(fieldSort::unmappedType).orElse(fieldSort));
        });
    }

    private SortOrder toSortOrder(Sort.Order sortOrder) {
        switch (sortOrder) {
            case ASC: return SortOrder.ASC;
            case DESC: return SortOrder.DESC;
            default: throw new IllegalStateException("Invalid sort order: " + sortOrder);
        }
    }
    private void applyHighlightingIfActivated(SearchSourceBuilder searchSourceBuilder, SearchJob job, Query query) {
        if (!allowHighlighting)
            return;

        final QueryStringQueryBuilder highlightQuery = decoratedHighlightQuery(job, query);

        searchSourceBuilder.highlighter(new HighlightBuilder().requireFieldMatch(false)
                .highlightQuery(highlightQuery)
                .field("*")
                .fragmentSize(0)
                .numOfFragments(0));
    }

    private QueryStringQueryBuilder decoratedHighlightQuery(SearchJob job, Query query) {
        final String raw = ((ElasticsearchQueryString) query.query()).queryString();

        final String decorated = this.esQueryDecorators.decorate(raw, job, query, Collections.emptySet());

        return QueryBuilders.queryStringQuery(decorated);
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, MessageList searchType, org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse result, Aggregations aggregations, ESGeneratedQueryContext queryContext) {
        final List<ResultMessageSummary> messages = StreamSupport.stream(result.getHits().spliterator(), false)
                .map(ESMessageList::resultMessageFromSearchHit)
                .map((resultMessage) -> ResultMessageSummary.create(resultMessage.highlightRanges, resultMessage.getMessage().getFields(), resultMessage.getIndex()))
                .collect(Collectors.toList());

        final String undecoratedQueryString = ((ElasticsearchQueryString)query.query()).queryString();
        final String queryString = this.esQueryDecorators.decorate(undecoratedQueryString, job, query, Collections.emptySet());

        final DateTime from = query.effectiveTimeRange(searchType).getFrom();
        final DateTime to = query.effectiveTimeRange(searchType).getTo();

        final SearchResponse searchResponse = SearchResponse.create(
                undecoratedQueryString,
                queryString,
                Collections.emptySet(),
                messages,
                Collections.emptySet(),
                0,
                result.getHits().getTotalHits().value,
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
