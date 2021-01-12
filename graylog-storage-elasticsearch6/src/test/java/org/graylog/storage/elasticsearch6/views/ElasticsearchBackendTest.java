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
package org.graylog.storage.elasticsearch6.views;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESMessageList;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ElasticsearchBackendTest {

    private static ElasticsearchBackend backend;

    @BeforeClass
    public static void setup() {
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, () -> new ESMessageList(new QueryStringDecorators.Fake()));

        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        final QueryStringParser queryStringParser = new QueryStringParser();
        backend = new ElasticsearchBackend(handlers,
                null,
                mock(IndexLookup.class),
                new QueryStringDecorators.Fake(),
                (elasticsearchBackend, ssb, job, query, results) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, results, fieldTypesLookup),
                false);
    }

    @Test
    public void generatesSearchForEmptySearchTypes() throws Exception {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.builder().queryString("").build())
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        backend.generate(job, query, Collections.emptySet());
    }

    @Test
    public void executesSearchForEmptySearchTypes() throws Exception {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.builder().queryString("").build())
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        final ESGeneratedQueryContext queryContext = mock(ESGeneratedQueryContext.class);

        final QueryResult queryResult = backend.doRun(job, query, queryContext, Collections.emptySet());

        assertThat(queryResult).isNotNull();
        assertThat(queryResult.searchTypes()).isEmpty();
        assertThat(queryResult.executionStats()).isNotNull();
        assertThat(queryResult.errors()).isEmpty();
    }
}
