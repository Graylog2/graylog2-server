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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.PositionTrackingQuery;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DecorateQueryStringsNormalizerTest {
    private final QueryStringDecorator dummyQueryStringDecorator = (queryString, parameterProvider, query) -> PositionTrackingQuery.of("Hey there!");
    private final DecorateQueryStringsNormalizer decorateQueryStringsNormalizer = new DecorateQueryStringsNormalizer(
            new QueryStringDecorators(Optional.of(dummyQueryStringDecorator))
    );

    @Test
    void decoratesQueryStrings() {
        final Query query = Query.builder()
                .query(ElasticsearchQueryString.of("action:index"))
                .build();


        final Query normalizedQuery = decorateQueryStringsNormalizer.normalizeQuery(query, name -> Optional.empty());

        final String normalizedQueryString = normalizedQuery.query().queryString();
        assertThat(normalizedQueryString).isEqualTo("Hey there!");

    }

    @Test
    void decoratesQueryStringFilters() {
        final Query query = Query.builder()
                .filter(QueryStringFilter.builder().query("action:index").build())
                .build();

        final Query normalizedQuery = decorateQueryStringsNormalizer.normalizeQuery(query, name -> Optional.empty());

        assertThat(normalizedQuery)
                .extracting(Query::filter)
                .allMatch(queryFilter -> (queryFilter instanceof QueryStringFilter && ((QueryStringFilter) queryFilter).query().equals("Hey there!")));
    }

    @Test
    void decoratesSearchTypes() {
        final Query query = Query.builder()
                .searchTypes(
                        Collections.singleton(MessageList.builder()
                                .query(ElasticsearchQueryString.of("action:index"))
                                .build())
                )
                .build();

        final Query normalizedQuery = decorateQueryStringsNormalizer.normalizeQuery(query, name -> Optional.empty());

        assertThat(normalizedQuery.searchTypes())
                .hasSize(1)
                .first()
                .extracting(searchType -> searchType.query().orElseThrow(IllegalStateException::new))
                .allMatch(q -> q instanceof BackendQuery && ((BackendQuery) q).queryString().equals("Hey there!"));
    }

    @Test
    void decoratesSearchTypeFilters() {
        final Query query = Query.builder()
                .searchTypes(
                        Collections.singleton(MessageList.builder()
                                .filter(QueryStringFilter.builder()
                                        .query("action:index")
                                        .build())
                                .build())
                )
                .build();


        final Query normalizedQuery = decorateQueryStringsNormalizer.normalizeQuery(query, name -> Optional.empty());

        assertThat(normalizedQuery.searchTypes())
                .hasSize(1)
                .extracting(SearchType::filter)
                .allMatch(filter -> (filter instanceof QueryStringFilter && ((QueryStringFilter) filter).query().equals("Hey there!")));
    }
}


