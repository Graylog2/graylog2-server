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
package org.graylog.plugins.views.search.engine;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryParserTest {
    private static QueryParser queryParser = new QueryParser(new QueryStringParser());

    @Test
    public void parse() throws Exception {
        final QueryMetadata queryMetadata = queryParser.parse(ImmutableSet.of(), Query.builder()
                .id("abc123")
                .query(ElasticsearchQueryString.builder().queryString("user_name:$username$ http_method:$foo$").build())
                .timerange(RelativeRange.create(600))
                .build());

        assertThat(queryMetadata.usedParameterNames())
                .containsExactlyInAnyOrder("username", "foo");
    }

    @Test
    public void parseAlsoConsidersWidgetFilters() throws Exception {
        final SearchType searchType1 = Pivot.builder()
                .id("searchType1")
                .filter(QueryStringFilter.builder().query("source:$bar$").build())
                .series(new ArrayList<>())
                .rollup(false)
                .build();
        final SearchType searchType2 = Pivot.builder()
                .id("searchType2")
                .filter(AndFilter.builder().filters(ImmutableSet.of(
                        QueryStringFilter.builder().query("http_action:$baz$").build(),
                        QueryStringFilter.builder().query("source:localhost").build()
                )).build())
                .series(new ArrayList<>())
                .rollup(false)
                .build();
        final QueryMetadata queryMetadata = queryParser.parse(ImmutableSet.of(), Query.builder()
                .id("abc123")
                .query(ElasticsearchQueryString.builder().queryString("user_name:$username$ http_method:$foo$").build())
                .timerange(RelativeRange.create(600))
                .searchTypes(ImmutableSet.of(searchType1, searchType2))
                .build());

        assertThat(queryMetadata.usedParameterNames())
                .containsExactlyInAnyOrder("username", "foo", "bar", "baz");
    }
}
