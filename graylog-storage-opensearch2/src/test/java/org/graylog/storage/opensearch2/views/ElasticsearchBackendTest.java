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
package org.graylog.storage.opensearch2.views;

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
import org.graylog.plugins.views.search.engine.SearchConfig;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.storage.opensearch2.views.searchtypes.ESMessageList;
import org.graylog.storage.opensearch2.views.searchtypes.ESSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.Period;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ElasticsearchBackendTest {
    private static ElasticsearchBackend backend;

    @BeforeClass
    public static void setup() {
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, () -> new ESMessageList(new QueryStringDecorators(Optional.empty())));

        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        backend = new ElasticsearchBackend(handlers,
                null,
                mock(IndexLookup.class),
                new QueryStringDecorators(Optional.empty()),
                (elasticsearchBackend, ssb, job, query) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, fieldTypesLookup),
                false);
    }

    @Test
    public void generatesSearchForEmptySearchTypes() throws Exception {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.of(""))
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        backend.generate(job, query, new SearchConfig(Period.ZERO));
    }

    @Test
    public void executesSearchForEmptySearchTypes() throws Exception {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.of(""))
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        final ESGeneratedQueryContext queryContext = mock(ESGeneratedQueryContext.class);

        final QueryResult queryResult = backend.doRun(job, query, queryContext);

        assertThat(queryResult).isNotNull();
        assertThat(queryResult.searchTypes()).isEmpty();
        assertThat(queryResult.executionStats()).isNotNull();
        assertThat(queryResult.errors()).isEmpty();
    }
}
