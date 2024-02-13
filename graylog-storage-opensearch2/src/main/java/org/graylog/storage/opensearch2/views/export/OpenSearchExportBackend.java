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
package org.graylog.storage.opensearch2.views.export;

import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.plugins.views.search.export.SimpleMessage;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.IndicesOptions;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.TermsQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHit;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.sort.SortOrder;
import org.graylog.storage.opensearch2.TimeRangeQueryFactory;
import org.graylog2.plugin.Message;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.matchAllQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.queryStringQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.termsQuery;

@SuppressWarnings("rawtypes")
public class OpenSearchExportBackend implements ExportBackend {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchExportBackend.class);

    private final IndexLookup indexLookup;
    private final RequestStrategy requestStrategy;
    private final boolean allowLeadingWildcard;

    private final UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper;

    @Inject
    public OpenSearchExportBackend(IndexLookup indexLookup,
                                   RequestStrategy requestStrategy,
                                   @Named("allow_leading_wildcard_searches") boolean allowLeadingWildcard,
                                   final UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper) {
        this.indexLookup = indexLookup;
        this.requestStrategy = requestStrategy;
        this.allowLeadingWildcard = allowLeadingWildcard;
        this.usedSearchFiltersToQueryStringsMapper = usedSearchFiltersToQueryStringsMapper;
    }

    @Override
    public void run(ExportMessagesCommand command, Consumer<SimpleMessageChunk> chunkCollector) {
        boolean isFirstChunk = true;
        int totalCount = 0;

        while (true) {
            List<SearchHit> hits = search(command);

            if (hits.isEmpty()) {
                publishChunk(chunkCollector, hits, command.fieldsInOrder(), command.timeZone(), SimpleMessageChunk.ChunkOrder.LAST);
                return;
            }

            boolean success = publishChunk(chunkCollector, hits, command.fieldsInOrder(), command.timeZone(), isFirstChunk ? SimpleMessageChunk.ChunkOrder.FIRST : SimpleMessageChunk.ChunkOrder.INTERMEDIATE);
            if (!success) {
                return;
            }

            totalCount += hits.size();
            if (command.limit().isPresent() && totalCount >= command.limit().getAsInt()) {
                LOG.info("Limit of {} reached. Stopping message retrieval.", command.limit().getAsInt());
                publishChunk(chunkCollector, Collections.emptyList(), command.fieldsInOrder(), command.timeZone(), SimpleMessageChunk.ChunkOrder.LAST);
                return;
            }

            isFirstChunk = false;
        }
    }

    private List<SearchHit> search(ExportMessagesCommand command) {
        SearchRequest search = prepareSearchRequest(command);

        return requestStrategy.nextChunk(search, command);
    }

    private SearchRequest prepareSearchRequest(ExportMessagesCommand command) {
        SearchSourceBuilder ssb = searchSourceBuilderFrom(command);

        Set<String> indices = indicesFor(command);
        return new SearchRequest()
                .source(ssb)
                .indices(indices.toArray(new String[0]))
                .indicesOptions(IndicesOptions.fromOptions(false, false, true, false));
    }

    private SearchSourceBuilder searchSourceBuilderFrom(ExportMessagesCommand command) {
        QueryBuilder query = queryFrom(command);

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(query)
                .size(command.chunkSize())
                .sort(Message.FIELD_TIMESTAMP, SortOrder.ASC);

        return requestStrategy.configure(ssb);
    }

    private QueryBuilder queryFrom(ExportMessagesCommand command) {
        final BoolQueryBuilder boolQueryBuilder = boolQuery()
                .filter(queryStringFilter(command.queryString()))
                .filter(timestampFilter(command))
                .filter(streamsFilter(command));

        final Collection<UsedSearchFilter> usedSearchFilters = command.usedSearchFilters();
        if (usedSearchFilters != null) {
            usedSearchFiltersToQueryStringsMapper.map(usedSearchFilters)
                    .forEach(filterQueryString -> boolQueryBuilder.filter(queryStringFilter(filterQueryString)));
        }
        return boolQueryBuilder;
    }

    private QueryBuilder queryStringFilter(final ElasticsearchQueryString backendQuery) {
        return backendQuery.isEmpty() ?
                matchAllQuery() :
                queryStringQuery(backendQuery.queryString()).allowLeadingWildcard(allowLeadingWildcard);
    }

    private QueryBuilder queryStringFilter(final String queryString) {
        ElasticsearchQueryString backendQuery = ElasticsearchQueryString.of(queryString);
        return queryStringFilter(backendQuery);
    }

    private QueryBuilder timestampFilter(ExportMessagesCommand command) {
        return requireNonNull(TimeRangeQueryFactory.create(command.timeRange()));
    }

    private TermsQueryBuilder streamsFilter(ExportMessagesCommand command) {
        return termsQuery(Message.FIELD_STREAMS, command.streams());
    }

    private Set<String> indicesFor(ExportMessagesCommand command) {
        return indexLookup.indexNamesForStreamsInTimeRange(command.streams(), command.timeRange());
    }

    private boolean publishChunk(Consumer<SimpleMessageChunk> chunkCollector, List<SearchHit> hits, LinkedHashSet<String> desiredFieldsInOrder, DateTimeZone timeZone, SimpleMessageChunk.ChunkOrder chunkOrder) {
        SimpleMessageChunk chunk = chunkFrom(hits, desiredFieldsInOrder, timeZone, chunkOrder);

        try {
            chunkCollector.accept(chunk);
            return true;
        } catch (Exception e) {
            LOG.warn("Chunk publishing threw exception. Stopping search after queries", e);
            return false;
        }
    }

    private SimpleMessageChunk chunkFrom(List<SearchHit> hits, LinkedHashSet<String> desiredFieldsInOrder, DateTimeZone timeZone, SimpleMessageChunk.ChunkOrder chunkOrder) {
        LinkedHashSet<SimpleMessage> messages = messagesFrom(hits, timeZone);

        return SimpleMessageChunk.builder()
                .fieldsInOrder(desiredFieldsInOrder)
                .messages(messages)
                .chunkOrder(chunkOrder)
                .build();
    }

    private LinkedHashSet<SimpleMessage> messagesFrom(List<SearchHit> hits, DateTimeZone timeZone) {
        return hits.stream()
                .map(h -> buildHitWithAllFields(h.getSourceAsMap(), h.getIndex(), timeZone))
                .collect(toCollection(LinkedHashSet::new));
    }

}
