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
package org.graylog.storage.elasticsearch7.views;

import com.google.common.collect.ImmutableList;
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
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESMessageList;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


public class ElasticsearchBackendTest {
    private ElasticsearchBackend backend;
    private UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper;

    @Before
    public void setup() {
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, ESMessageList::new);

        usedSearchFiltersToQueryStringsMapper = mock(UsedSearchFiltersToQueryStringsMapper.class);
        doReturn(Collections.emptySet()).when(usedSearchFiltersToQueryStringsMapper).map(any());
        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        backend = new ElasticsearchBackend(handlers,
                null,
                mock(IndexLookup.class),
                new QueryStringDecorators(Optional.empty()),
                (elasticsearchBackend, ssb, job, query, errors) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, errors, fieldTypesLookup),
                usedSearchFiltersToQueryStringsMapper,
                false);
    }

    @Test
    public void generatesSearchForEmptySearchTypes() {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.of(""))
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        backend.generate(job, query, Collections.emptySet());
    }

    @Test
    public void executesSearchForEmptySearchTypes() {
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

    @Test
    public void generatedContextHasQueryThatIncludesSearchFilters() {
        final ImmutableList<UsedSearchFilter> usedSearchFilters = ImmutableList.of(
                InlineQueryStringSearchFilter.builder().title("").description("").queryString("method:GET").build(),
                ReferencedQueryStringSearchFilter.create("12345")
        );
        doReturn(ImmutableSet.of("method:GET", "method:POST")).when(usedSearchFiltersToQueryStringsMapper).map(usedSearchFilters);

        final Query query = Query.builder()
                .id("queryWithSearchFilters")
                .query(ElasticsearchQueryString.of(""))
                .filters(usedSearchFilters)
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        final ESGeneratedQueryContext queryContext = backend.generate(job, query, Collections.emptySet());
        final QueryBuilder esQuery = queryContext.searchSourceBuilder(new SearchType.Fallback()).query();
        assertThat(esQuery)
                .isNotNull()
                .isInstanceOf(BoolQueryBuilder.class);

        final List<QueryBuilder> filters = ((BoolQueryBuilder) esQuery).filter();

        //filter for empty ES query
        assertThat(filters)
                .anyMatch(queryBuilder -> queryBuilder instanceof MatchAllQueryBuilder);

        //2 filters from search filters
        assertThat(filters)
                .filteredOn(queryBuilder -> queryBuilder instanceof QueryStringQueryBuilder)
                .extracting(queryBuilder -> (QueryStringQueryBuilder) queryBuilder)
                .extracting(QueryStringQueryBuilder::queryString)
                .contains("method:POST")
                .contains("method:GET");
    }
}
