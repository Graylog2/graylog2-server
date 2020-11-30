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
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendSearchTypeOverridesTest extends ElasticsearchBackendGeneratedRequestTestBase {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SearchJob searchJob;
    private Query query;

    @Before
    public void setUpFixtures() throws InvalidRangeParametersException {
        final Set<SearchType> searchTypes = new HashSet<SearchType>() {{
            add(
                    Pivot.builder()
                            .id("pivot1")
                            .series(Collections.singletonList(Average.builder().field("field1").build()))
                            .rollup(true)
                            .timerange(DerivedTimeRange.of(AbsoluteRange.create("2019-09-11T10:31:52.819Z", "2019-09-11T10:36:52.823Z")))
                            .build()
            );
            add(
                    Pivot.builder()
                            .id("pivot2")
                            .series(Collections.singletonList(Max.builder().field("field2").build()))
                            .rollup(true)
                            .query(ElasticsearchQueryString.builder().queryString("source:babbage").build())
                            .build()
            );
        }};
        this.query = Query.builder()
                .id("query1")
                .searchTypes(searchTypes)
                .query(ElasticsearchQueryString.builder().queryString("production:true").build())
                .filter(StreamFilter.ofId("stream1"))
                .timerange(timeRangeForTest())
                .build();

        this.searchJob = searchJobForQuery(this.query);
    }

    @Test
    public void overridesInSearchTypeAreIncorporatedIntoGeneratedQueries() throws IOException {
        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());
        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("successfulMultiSearchResponse.json")));

        final String generatedRequest = run(searchJob, query, queryContext, Collections.emptySet());

        assertThat(generatedRequest).isEqualTo(resourceFile("overridesInSearchTypeAreIncorporatedIntoGeneratedQueries.request.ndjson"));
    }

    @Test
    public void timerangeOverridesAffectIndicesSelection() throws IOException, InvalidRangeParametersException {
        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("stream1"), timeRangeForTest()))
                .thenReturn(ImmutableSet.of("queryIndex"));

        TimeRange tr = AbsoluteRange.create("2019-09-11T10:31:52.819Z", "2019-09-11T10:36:52.823Z");
        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("stream1"), tr))
                .thenReturn(ImmutableSet.of("searchTypeIndex"));

        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());
        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("successfulMultiSearchResponse.json")));

        final String generatedRequest = run(searchJob, query, queryContext, Collections.emptySet());

        assertThat(generatedRequest).isEqualTo(resourceFile("timerangeOverridesAffectIndicesSelection.request.ndjson"));
    }
}
