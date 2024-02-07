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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;
import jakarta.inject.Provider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.monitoring.collection.NoOpStatsCollector;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.MatchAllQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryStringQueryBuilder;
import org.graylog.storage.opensearch2.views.searchtypes.OSMessageList;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.EffectiveTimeRangeExtractor;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets.OSTimeHandler;
import org.graylog.testing.jsonpath.JsonPathAssert;
import org.graylog2.indexer.ranges.MongoIndexRange;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Tools.nowUTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenSearchBackendTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private OpenSearchBackend backend;
    private UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper;

    @Mock
    private IndexLookup indexLookup;

    @Before
    public void setup() {
        Map<String, Provider<OSSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, OSMessageList::new);
        handlers.put(Pivot.NAME, () -> new OSPivot(Map.of(Time.NAME, new OSTimeHandler()), Map.of(), new EffectiveTimeRangeExtractor()));

        usedSearchFiltersToQueryStringsMapper = mock(UsedSearchFiltersToQueryStringsMapper.class);
        doReturn(Collections.emptySet()).when(usedSearchFiltersToQueryStringsMapper).map(any());
        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);

        backend = new OpenSearchBackend(handlers,
                null,
                indexLookup,
                (elasticsearchBackend, ssb, errors) -> new OSGeneratedQueryContext(elasticsearchBackend, ssb, errors, fieldTypesLookup),
                usedSearchFiltersToQueryStringsMapper,
                new NoOpStatsCollector<>(),
                false);
    }

    @Test
    public void generatesSearchForEmptySearchTypes() {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.of(""))
                .timerange(RelativeRange.create(300))
                .build();
        backend.generate(query, Collections.emptySet());
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

        final OSGeneratedQueryContext queryContext = mock(OSGeneratedQueryContext.class);

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

        final OSGeneratedQueryContext queryContext = backend.generate(query, Collections.emptySet());
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

    @Test
    public void testExplain() {
        when(indexLookup.indexRangesForStreamsInTimeRange(anySet(), any())).thenAnswer(a -> {
            if (a.getArgument(1, TimeRange.class).getFrom().getYear() < 2024) {
                return Set.of(
                        MongoIndexRange.create("graylog_0", nowUTC(), nowUTC(), nowUTC(), 0),
                        MongoIndexRange.create("graylog_1", nowUTC(), nowUTC(), nowUTC(), 0),
                        MongoIndexRange.create("graylog_warm_2", nowUTC(), nowUTC(), nowUTC(), 0)
                );
            }
            return Set.of(MongoIndexRange.create("graylog_0", nowUTC(), nowUTC(), nowUTC(), 0));
        });

        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.of("needle"))
                .searchTypes(Set.of(
                                MessageList.builder()
                                        .id("messagelist-1")
                                        .build(),
                                Pivot.builder()
                                        .id("pivot-1")
                                        .rowGroups(Time.builder().field("source").interval(AutoInterval.create()).build())
                                        .timerange(AbsoluteRange.create(DateTime.parse("2016-05-19T00:00:00.000Z"), DateTime.parse("2022-01-09T00:00:00.000Z")))
                                        .series()
                                        .rollup(false)
                                        .build()
                        )
                )
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");
        final GeneratedQueryContext generatedQueryContext = backend.generate(query, Set.of());

        var explainResult = backend.explain(job, query, generatedQueryContext);
        assertThat(explainResult.searchTypes()).isNotNull();
        assertThat(explainResult.searchTypes().get("messagelist-1")).satisfies(ml -> {
            assertThat(ml).isNotNull();

            assertThat(ml.searchedIndexRanges()).hasSize(1);
            assertThat(ml.searchedIndexRanges()).allMatch(r -> r.indexName().equals("graylog_0"));


            var ctx = JsonPath.parse(ml.queryString());
            JsonPathAssert.assertThat(ctx).jsonPathAsString("$.query.bool.must[0].bool.filter[0].query_string.query").isEqualTo("needle");
        });

        assertThat(explainResult.searchTypes().get("pivot-1")).satisfies(ml -> {
            assertThat(ml).isNotNull();
            assertThat(ml.searchedIndexRanges()).hasSize(3);
            assertThat(ml.searchedIndexRanges()).anyMatch(r -> r.indexName().equals("graylog_0") && !r.isWarmTiered());
            assertThat(ml.searchedIndexRanges()).anyMatch(r -> r.indexName().equals("graylog_warm_2") && r.isWarmTiered());

            var ctx = JsonPath.parse(ml.queryString());
            JsonPathAssert.assertThat(ctx).jsonPathAsString("$.query.bool.must[0].bool.filter[0].query_string.query").isEqualTo("needle");
            JsonPathAssert.assertThat(ctx).jsonPathAsString("$.aggregations.agg.date_histogram.field").isEqualTo("source");
        });
    }

}
