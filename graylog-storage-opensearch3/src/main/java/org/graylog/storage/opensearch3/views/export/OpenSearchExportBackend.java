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
package org.graylog.storage.opensearch3.views.export;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.export.ExportException;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.plugins.views.search.export.SimpleMessage;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.TimeRangeQueryFactory;
import org.graylog2.database.filtering.AttributeFilter;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.plugin.Message;
import org.joda.time.DateTimeZone;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.util.ObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;

@SuppressWarnings("rawtypes")
public class OpenSearchExportBackend implements ExportBackend {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchExportBackend.class);

    private static final String DEFAULT_TIEBREAKER_FIELD = Message.GL2_SECOND_SORT_FIELD;

    private final IndexLookup indexLookup;

    private final boolean allowLeadingWildcard;

    private final UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper;

    private final OfficialOpensearchClient opensearchClient;

    @Inject
    public OpenSearchExportBackend(IndexLookup indexLookup,
                                   @Named("allow_leading_wildcard_searches") boolean allowLeadingWildcard,
                                   final UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper,
                                   OfficialOpensearchClient opensearchClient) {
        this.indexLookup = indexLookup;
        this.opensearchClient = opensearchClient;
        this.allowLeadingWildcard = allowLeadingWildcard;
        this.usedSearchFiltersToQueryStringsMapper = usedSearchFiltersToQueryStringsMapper;
    }

    @Override
    public void run(ExportMessagesCommand command, Consumer<SimpleMessageChunk> chunkCollector) {
        boolean isFirstChunk = true;
        int totalCount = 0;

        SearchAfterValues searchAfterValues = SearchAfterValues.empty();

        while (true) {
            List<Hit<Map>> hits = search(command, searchAfterValues);

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

    private List<Hit<Map>> search(ExportMessagesCommand command, SearchAfterValues searchAfterValues) {
        SearchResponse<Map> result = doSearch(createSearchRequest(command, searchAfterValues));
        searchAfterValues.update(result);
        return result.hits().hits();
    }

    private SearchResponse<Map> doSearch(SearchRequest searchRequest) {
        try {
            final SearchResponse<Map> result = opensearchClient.sync(c -> c.search(searchRequest, Map.class), "failed to serarch");
            if (result.shards().failed() > 0) {
                final List<String> errors = result.shards().failures().stream()
                        .map(e -> e.index() + ": " + e.reason().reason())
                        .distinct()
                        .toList();
                throw new ElasticsearchException("Unable to perform export query: ", errors);
            }
            return result;
        } catch (Throwable e) {
            throw new ExportException("Unable to complete export: ", new ElasticsearchException(e));
        }
    }

    private SearchRequest createSearchRequest(ExportMessagesCommand command, SearchAfterValues searchAfterValues) {
        return SearchRequest.of(builder -> {

            getIndices(command).ifPresent(builder::index);

            builder.ignoreUnavailable(true);
            builder.allowNoIndices(false);
            builder.expandWildcards(ExpandWildcard.Open);

            builder.size(command.chunkSize())
                    .query(query(command))
                    .sort(s -> s.field(f -> f.field("timestamp").order(SortOrder.Asc)))
                    .sort(s -> s.field(f -> f.field(DEFAULT_TIEBREAKER_FIELD).order(SortOrder.Asc).unmappedType(FieldType.Keyword)));

            if (!command.exportAllFields()) {
                builder.source(s -> s.filter(sf -> sf.includes(new LinkedList<>(command.fieldsInOrder()))));
            }

            searchAfterValues.ifPresent(builder::searchAfter);

            return builder;
        });
    }

    @Nonnull
    private Optional<List<String>> getIndices(ExportMessagesCommand command) {
        final Set<String> indexNames = indexLookup.indexNamesForStreamsInTimeRange(command.streams(), command.timeRange());
        return Optional.of(indexNames)
                .filter(set -> !set.isEmpty())
                .map(LinkedList::new);
    }

    private Query query(ExportMessagesCommand command) {
        final ObjectBuilder<Query> boolQuery = Query.builder().bool(builder -> {
                    builder
                            .filter(queryStringFilter(command.queryString()))
                            .filter(timestampFilter(command))
                            .filter(streamsFilter(command));

                    final List<AttributeFilter> attributeFilters = command.attributeFilters();
                    if (attributeFilters != null && !attributeFilters.isEmpty()) {
                        attributeFilters.stream()
                                .flatMap(attribute -> attribute.toQueryStrings().stream())
                                .forEach(filterQuery -> builder.filter(q -> q.queryString(qs -> qs.query(filterQuery))));
                    }


                    final Collection<UsedSearchFilter> usedSearchFilters = command.usedSearchFilters();
                    if (usedSearchFilters != null) {
                        usedSearchFiltersToQueryStringsMapper.map(usedSearchFilters)
                                .forEach(filterQueryString -> builder.filter(q -> q.queryString(qs -> qs.query(filterQueryString))));
                    }
                    return builder;
                }
        );
        return boolQuery.build();
    }


    private Query queryStringFilter(final ElasticsearchQueryString backendQuery) {
        return backendQuery.isEmpty() ?
                Query.builder().matchAll(q -> q).build() :
                Query.builder().queryString(q -> q.allowLeadingWildcard(allowLeadingWildcard).query(backendQuery.queryString())).build();
    }


    private Query timestampFilter(ExportMessagesCommand command) {
        final RangeQuery timeRangeQuery = TimeRangeQueryFactory.createTimeRangeQuery(command.timeRange());
        return Query.builder().range(timeRangeQuery).build();
    }

    private Query streamsFilter(ExportMessagesCommand command) {
        return Query.builder().terms(termsQuery -> {
            termsQuery.field(Message.FIELD_STREAMS);
            termsQuery.terms(terms -> terms.value(termsFromStreams(command.streams())));
            return termsQuery;
        }).build();
    }

    private List<FieldValue> termsFromStreams(Set<String> streams) {
        return streams.stream()
                .map(stream -> new FieldValue.Builder().stringValue(stream).build())
                .collect(Collectors.toList());
    }

    private boolean publishChunk(Consumer<SimpleMessageChunk> chunkCollector, List<Hit<Map>> hits, LinkedHashSet<String> desiredFieldsInOrder, DateTimeZone timeZone, SimpleMessageChunk.ChunkOrder chunkOrder) {
        SimpleMessageChunk chunk = chunkFrom(hits, desiredFieldsInOrder, timeZone, chunkOrder);

        try {
            chunkCollector.accept(chunk);
            return true;
        } catch (Exception e) {
            LOG.warn("Chunk publishing threw exception. Stopping search after queries", e);
            return false;
        }
    }

    private SimpleMessageChunk chunkFrom(List<Hit<Map>> hits, LinkedHashSet<String> desiredFieldsInOrder, DateTimeZone timeZone, SimpleMessageChunk.ChunkOrder chunkOrder) {
        LinkedHashSet<SimpleMessage> messages = messagesFrom(hits, timeZone);

        return SimpleMessageChunk.builder()
                .fieldsInOrder(desiredFieldsInOrder)
                .messages(messages)
                .chunkOrder(chunkOrder)
                .build();
    }

    private LinkedHashSet<SimpleMessage> messagesFrom(List<Hit<Map>> hits, DateTimeZone timeZone) {
        return hits.stream()
                .map(h -> buildHitWithAllFields(h.source(), h.index(), timeZone))
                .collect(toCollection(LinkedHashSet::new));
    }

}
