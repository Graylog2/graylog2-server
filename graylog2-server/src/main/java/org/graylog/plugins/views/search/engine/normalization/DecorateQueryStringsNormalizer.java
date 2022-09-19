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

import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.filter.QueryStringFilter;

import javax.inject.Inject;
import java.util.stream.Collectors;

public class DecorateQueryStringsNormalizer implements SearchNormalizer {
    private final QueryStringDecorators queryStringDecorators;

    @Inject
    public DecorateQueryStringsNormalizer(QueryStringDecorators queryStringDecorators) {
        this.queryStringDecorators = queryStringDecorators;
    }

    @Override
    public Query normalizeQuery(final Query query, final ParameterProvider parameterProvider) {
        return query.toBuilder()
                .query(ElasticsearchQueryString.of(this.queryStringDecorators.decorate(query.query().queryString(), parameterProvider, query)))
                .filter(normalizeFilter(query.filter(), query, parameterProvider))
                .searchTypes(query.searchTypes().stream().map(searchType -> normalizeSearchType(searchType, query, parameterProvider)).collect(Collectors.toSet()))
                .build();
    }

    private Filter normalizeFilter(Filter filter, Query query, ParameterProvider parameterProvider) {
        if (filter == null) {
            return filter;
        }
        Filter normalizedFilter = filter;
        if (filter instanceof QueryStringFilter queryStringFilter) {
            normalizedFilter = queryStringFilter.withQuery(this.queryStringDecorators.decorate(queryStringFilter.query(), parameterProvider, query));
        }

        if (normalizedFilter.filters() == null) {
            return normalizedFilter;
        }

        return normalizedFilter.withFilters(normalizedFilter.filters()
                .stream()
                .map(f -> normalizeFilter(f, query, parameterProvider))
                .collect(Collectors.toSet()));
    }

    private SearchType normalizeSearchType(SearchType searchType, Query query, ParameterProvider parameterProvider) {
        final SearchType searchTypeWithNormalizedQuery = searchType.query()
                .map(backendQuery -> searchType.withQuery(ElasticsearchQueryString.of(this.queryStringDecorators.decorate(backendQuery.queryString(), parameterProvider, query))))
                .orElse(searchType);

        return searchTypeWithNormalizedQuery.withFilter(normalizeFilter(searchType.filter(), query, parameterProvider));
    }

}
