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
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.search.QueryStringUtils;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;

import java.util.Optional;

/**
 * Opensearch 3 version of {@link SearchRequestFactory}, that will eventually replace its deprecated predecessor.
 * Currently, it does not cover all the necessary functionality.
 */
public class SearchRequestFactoryOS {

    private final boolean allowLeadingWildcardSearches;

    @Inject
    public SearchRequestFactoryOS(@Named("allow_leading_wildcard_searches") final boolean allowLeadingWildcardSearches) {
        this.allowLeadingWildcardSearches = allowLeadingWildcardSearches;
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

}
