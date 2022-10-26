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
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSAverageHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSMaxHandler;
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
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OpenSearchBackendGeneratedRequestTestBase {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    OpenSearchBackend openSearchBackend;

    @Mock
    protected OpenSearchClient client;

    @Mock
    protected IndexLookup indexLookup;

    @Mock
    protected FieldTypesLookup fieldTypesLookup;

    protected Map<String, Provider<OSSearchTypeHandler<? extends SearchType>>> elasticSearchTypeHandlers;

    @Captor
    protected ArgumentCaptor<List<SearchRequest>> clientRequestCaptor;

    @Before
    public void setUpSUT() {
        this.elasticSearchTypeHandlers = new HashMap<>();
        final Map<String, OSPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers = Collections.emptyMap();
        final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers = new HashMap<>();
        seriesHandlers.put(Average.NAME, new OSAverageHandler());
        seriesHandlers.put(Max.NAME, new OSMaxHandler());
        elasticSearchTypeHandlers.put(Pivot.NAME, () -> new OSPivot(bucketHandlers, seriesHandlers));

        this.openSearchBackend = new OpenSearchBackend(elasticSearchTypeHandlers,
                client,
                indexLookup,
                (elasticsearchBackend, ssb, errors) -> new OSGeneratedQueryContext(elasticsearchBackend, ssb, errors, fieldTypesLookup),
                usedSearchFilters -> usedSearchFilters.stream()
                        .filter(sf -> sf instanceof InlineQueryStringSearchFilter)
                        .map(inlineSf -> ((InlineQueryStringSearchFilter) inlineSf).queryString())
                        .collect(Collectors.toSet()),
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

    List<SearchRequest> run(SearchJob searchJob, Query query, OSGeneratedQueryContext queryContext, Set<QueryResult> predecessorResults) {
        this.openSearchBackend.doRun(searchJob, query, queryContext);

        verify(client, times(1)).msearch(clientRequestCaptor.capture(), any());

        return clientRequestCaptor.getValue();
    }
}
