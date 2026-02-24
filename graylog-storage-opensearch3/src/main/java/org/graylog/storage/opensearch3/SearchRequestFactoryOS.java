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
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.search.QueryStringUtils;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Opensearch 3 version of {@link SearchRequestFactory}, that will eventually replace its deprecated predecessor.
 */
public class SearchRequestFactoryOS {

    private final boolean allowLeadingWildcardSearches;
    private final UsedSearchFiltersToQueryStringsMapper searchFiltersMapper;

    @Inject
    public SearchRequestFactoryOS(@Named("allow_leading_wildcard_searches") final boolean allowLeadingWildcardSearches,
                                  final UsedSearchFiltersToQueryStringsMapper searchFiltersMapper) {
        this.allowLeadingWildcardSearches = allowLeadingWildcardSearches;
        this.searchFiltersMapper = searchFiltersMapper;
    }

    public Query createQuery(final String queryString,
                             final Optional<TimeRange> range,
                             final Optional<String> filter) {
        final String query = QueryStringUtils.normalizeQuery(queryString);
        BoolQuery.Builder topQueryBuilder;
        if (QueryStringUtils.isEmptyOrMatchAllQueryString(query)) {
            topQueryBuilder = QueryBuilders.bool()
                    .must(
                            QueryBuilders.matchAll()
                                    .build()
                                    .toQuery()
                    );
        } else {
            topQueryBuilder = QueryBuilders.bool()
                    .must(
                            QueryBuilders.queryString()
                                    .query(query)
                                    .allowLeadingWildcard(allowLeadingWildcardSearches)
                                    .build()
                                    .toQuery()
                    );
        }

        final Optional<RangeQuery> rangeQuery = range
                .map(TimeRangeQueryFactory::createTimeRangeQuery);
        final Optional<Query> filterQuery = filter
                .filter(f -> !QueryStringUtils.isEmptyOrMatchAllQueryString(f))
                .map(f -> QueryBuilders.queryString().query(f).build().toQuery());


        filterQuery.ifPresent(topQueryBuilder::filter);
        rangeQuery.ifPresent(rQuery -> topQueryBuilder.filter(rQuery.toQuery()));
        return topQueryBuilder.build().toQuery();
    }

    /**
     * Creates a query from a SearchCommand with all filters applied.
     */
    public Query createQuery(final SearchCommand searchCommand) {
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

        return Query.of(q -> q.bool(boolQuery.build()));
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

}
