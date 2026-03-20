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
package org.graylog.storage.opensearch3;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.storage.search.SearchCommand;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.search.QueryStringUtils;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Opensearch 3 version of {@link SearchRequestFactory}, that will eventually replace its deprecated predecessor.
 */
public class SearchRequestFactoryOS {

    private static final Sorting DEFAULT_SORTING = new Sorting("_doc", Sorting.Direction.ASC);
    private final boolean allowHighlighting;
    private final boolean allowLeadingWildcardSearches;
    private final UsedSearchFiltersToQueryStringsMapper searchFiltersMapper;

    @Inject
    public SearchRequestFactoryOS(@Named("allow_highlighting") boolean allowHighlighting,
                                  @Named("allow_leading_wildcard_searches") final boolean allowLeadingWildcardSearches,
                                  final UsedSearchFiltersToQueryStringsMapper searchFiltersMapper) {
        this.allowHighlighting = allowHighlighting;
        this.allowLeadingWildcardSearches = allowLeadingWildcardSearches;
        this.searchFiltersMapper = searchFiltersMapper;
    }

    public SearchRequest.Builder create(final ChunkCommand chunkCommand) {
        SearchRequest.Builder builder = create(SearchCommand.from(chunkCommand));
        // Set source fields
        if (!chunkCommand.fields().isEmpty()) {
            builder.source(s -> s.filter(sf -> sf.includes(new LinkedList<>(chunkCommand.fields()))));
        }

        // Set slice parameters
        chunkCommand.sliceParams().ifPresent(sliceParams ->
                builder.slice(slice -> slice.id(sliceParams.id()).max(sliceParams.max()))
        );

        // let batchSize override already set limit if present
        // IMPORTANT: Don't use 'from' (offset) with slice parameters - they're incompatible in OpenSearch/Elasticsearch
        // Combining them can cause incorrect results and duplicate hit counts
        if (chunkCommand.sliceParams().isPresent()) {
            builder.from(null);
        }
        chunkCommand.batchSize().ifPresent(batchSize -> builder.size(Math.toIntExact(batchSize)));
        return builder;
    }

    /**
     * Creates a query from a SearchCommand with all filters applied.
     */
    public SearchRequest.Builder create(final SearchCommand searchCommand) {
        final BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Main query
        final String queryString = QueryStringUtils.normalizeQuery(searchCommand.query());
        boolQuery.must(translateQueryString(queryString));

        // Time range filter
        searchCommand.range()
                .map(TimeRangeQueryFactory::createTimeRangeQuery)
                .ifPresent(rangeQuery -> boolQuery.filter(Query.of(q -> q.range(rangeQuery))));

        // Additional filter
        searchCommand.filter()
                .filter(filter -> !QueryStringUtils.isEmptyOrMatchAllQueryString(filter))
                .ifPresent(filter -> boolQuery.filter(translateQueryString(filter)));

        // Streams filter
        searchCommand.streams()
                .map(this::buildStreamIdFilter)
                .ifPresent(boolQuery::filter);

        // Search filters
        searchFiltersMapper.map(searchCommand.filters())
                .stream()
                .map(this::translateQueryString)
                .forEach(boolQuery::filter);

        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(Query.of(q -> q.bool(boolQuery.build())))
                .trackTotalHits(t -> t.enabled(true));

        applyPaginationIfPresent(builder, searchCommand);

        applySortingIfPresent(builder, searchCommand);

        applyHighlighting(builder, searchCommand);

        return builder;
    }

    /**
     * Translates a query string into a Query object.
     */
    public Query translateQueryString(final String queryString) {
        if (QueryStringUtils.isEmptyOrMatchAllQueryString(queryString)) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.queryString(qs ->
                qs.query(queryString).allowLeadingWildcard(allowLeadingWildcardSearches)
        ));
    }

    /**
     * Creates a terms query for the given field and values.
     */
    public Query termsQuery(final String field, final Set<String> values) {
        final List<FieldValue> fieldValues = values.stream()
                .map(FieldValue::of)
                .toList();
        return Query.of(q -> q.terms(t -> t
                .field(field)
                .terms(TermsQueryField.of(tqf -> tqf.value(fieldValues)))
        ));
    }

    /**
     * Creates a stream ID filter that handles both regular streams and the default stream.
     */
    Query buildStreamIdFilter(final Set<String> streams) {
        final BoolQuery.Builder filterBuilder = new BoolQuery.Builder();

        // If the included streams set contains the default stream, we also want all documents which do not
        // have any stream assigned. Those documents have basically been in the "default stream" which didn't
        // exist in Graylog <2.2.0.
        if (streams.contains(Stream.DEFAULT_STREAM_ID)) {
            filterBuilder.should(Query.of(q -> q.bool(b -> b.mustNot(mn -> mn.exists(e -> e.field(Message.FIELD_STREAMS))))));
        }

        // Only select messages which are assigned to the given streams
        filterBuilder.should(termsQuery(Message.FIELD_STREAMS, streams));

        return Query.of(q -> q.bool(filterBuilder.build()));
    }

    private void applyPaginationIfPresent(SearchRequest.Builder builder, SearchCommand searchCommand) {
        searchCommand.offset().ifPresent(builder::from);
        searchCommand.limit().ifPresent(builder::size);
    }

    private void applySortingIfPresent(SearchRequest.Builder builder, SearchCommand searchCommand) {
        final Sorting sorting = searchCommand.sorting().orElse(DEFAULT_SORTING);
        final SortOrder sortOrder = sorting.getDirection() == Sorting.Direction.ASC
                ? SortOrder.Asc
                : SortOrder.Desc;
        builder.sort(sort -> sort.field(field -> field
                .field(sorting.getField())
                .order(sortOrder)
        ));
    }

    private void applyHighlighting(final SearchRequest.Builder builder, final SearchCommand searchCommand) {
        if (allowHighlighting && searchCommand.highlight()) {
            builder.highlight(h -> h
                    .requireFieldMatch(false)
                    .fields("*", f -> f.fragmentSize(0).numberOfFragments(0))
            );
        }
    }
}
