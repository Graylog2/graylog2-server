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
package org.graylog.plugins.views.search.engine.normalization;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

public class DecorateQueryStringsNormalizer implements SearchNormalizer {
    private final QueryStringDecorators queryStringDecorators;

    @Inject
    public DecorateQueryStringsNormalizer(QueryStringDecorators queryStringDecorators) {
        this.queryStringDecorators = queryStringDecorators;
    }

    private Query normalizeQuery(Query query, Search search) {
        return query.toBuilder()
                .query(ElasticsearchQueryString.of(this.queryStringDecorators.decorate(query.query().queryString(), search, query)))
                .filter(normalizeFilter(query.filter(), query, search))
                .searchTypes(query.searchTypes().stream().map(searchType -> normalizeSearchType(searchType, query, search)).collect(Collectors.toSet()))
                .build();
    }

    private Filter normalizeFilter(Filter filter, Query query, Search search) {
        if (filter == null) {
            return filter;
        }
        Filter normalizedFilter = filter;
        if (filter instanceof QueryStringFilter) {
            final QueryStringFilter queryStringFilter = (QueryStringFilter) filter;
            normalizedFilter = queryStringFilter.withQuery(this.queryStringDecorators.decorate(queryStringFilter.query(), search, query));
        }

        if (normalizedFilter.filters() == null) {
            return normalizedFilter;
        }

        return normalizedFilter.withFilters(normalizedFilter.filters()
                .stream()
                .map(f -> normalizeFilter(f, query, search))
                .collect(Collectors.toSet()));
    }

    private SearchType normalizeSearchType(SearchType searchType, Query query, Search search) {
        final SearchType searchTypeWithNormalizedQuery = searchType.query()
                .map(backendQuery -> searchType.withQuery(ElasticsearchQueryString.of(this.queryStringDecorators.decorate(backendQuery.queryString(), search, query))))
                .orElse(searchType);

        return searchTypeWithNormalizedQuery.withFilter(normalizeFilter(searchType.filter(), query, search));
    }

    public Search normalize(Search search, SearchUser searchUser, ExecutionState executionState) {
        final ImmutableSet<Query> newQueries = search.queries().stream()
                .map(query -> normalizeQuery(query, search))
                .collect(ImmutableSet.toImmutableSet());
        return search.toBuilder().queries(newQueries).build();
    }
}
