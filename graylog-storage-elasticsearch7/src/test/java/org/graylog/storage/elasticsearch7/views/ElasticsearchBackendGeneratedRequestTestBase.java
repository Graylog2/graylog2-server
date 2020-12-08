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

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESAverageHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESMaxHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ElasticsearchBackendGeneratedRequestTestBase {
    protected static final QueryStringParser queryStringParser = new QueryStringParser();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    ElasticsearchBackend elasticsearchBackend;

    @Mock
    protected ElasticsearchClient client;

    @Mock
    protected IndexLookup indexLookup;

    @Mock
    protected FieldTypesLookup fieldTypesLookup;

    protected Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticSearchTypeHandlers;

    @Captor
    protected ArgumentCaptor<List<SearchRequest>> clientRequestCaptor;

    @Before
    public void setUpSUT() {
        this.elasticSearchTypeHandlers = new HashMap<>();
        final Map<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers = Collections.emptyMap();
        final Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers = new HashMap<>();
        seriesHandlers.put(Average.NAME, new ESAverageHandler());
        seriesHandlers.put(Max.NAME, new ESMaxHandler());
        elasticSearchTypeHandlers.put(Pivot.NAME, () -> new ESPivot(bucketHandlers, seriesHandlers));

        this.elasticsearchBackend = new ElasticsearchBackend(elasticSearchTypeHandlers,
                queryStringParser,
                client,
                indexLookup,
                new QueryStringDecorators.Fake(),
                (elasticsearchBackend, ssb, job, query, results) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, results, fieldTypesLookup),
                false);
    }

    SearchJob searchJobForQuery(Query query) {
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();
        return new SearchJob("job1", search, "admin");
    }

    TimeRange timeRangeForTest() {
        try {
            return AbsoluteRange.create("2018-08-23T10:02:00.247+02:00", "2018-08-23T10:07:00.252+02:00");
        } catch (InvalidRangeParametersException ignored) {
        }
        return null;
    }

    List<SearchRequest> run(SearchJob searchJob, Query query, ESGeneratedQueryContext queryContext, Set<QueryResult> predecessorResults) {
        this.elasticsearchBackend.doRun(searchJob, query, queryContext, predecessorResults);

        verify(client, times(1)).msearch(clientRequestCaptor.capture(), any());

        return clientRequestCaptor.getValue();
    }
}
