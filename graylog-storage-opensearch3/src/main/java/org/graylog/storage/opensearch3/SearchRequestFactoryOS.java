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
import org.graylog.storage.search.SearchCommand;
import org.graylog2.search.QueryStringUtils;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;

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

    public Query createQuery(final SearchCommand searchCommand) {
        final String query = QueryStringUtils.normalizeQuery(searchCommand.query());
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


        final Optional<BoolQuery.Builder> rangeQueryBuilder = searchCommand.range()
                .map(TimeRangeQueryFactory::createTimeRangeQuery)
                .map(rangeQuery -> QueryBuilders.bool().must(rangeQuery.toQuery()));
        final Optional<BoolQuery.Builder> filterQueryBuilder = searchCommand.filter()
                .filter(filter -> !QueryStringUtils.isEmptyOrMatchAllQueryString(filter))
                .map(filter -> QueryBuilders.queryString().query(filter).build().toQuery())
                .map(queryStringQuery -> QueryBuilders.bool().must(queryStringQuery));


        filterQueryBuilder.ifPresent(builder -> topQueryBuilder.filter(builder.build().toQuery()));
        rangeQueryBuilder.ifPresent(builder -> topQueryBuilder.filter(builder.build().toQuery()));
        return topQueryBuilder.build().toQuery();
    }

}
