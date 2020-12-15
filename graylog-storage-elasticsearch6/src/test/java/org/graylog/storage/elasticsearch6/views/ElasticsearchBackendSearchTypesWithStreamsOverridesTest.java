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
import io.searchbox.core.MultiSearch;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendSearchTypesWithStreamsOverridesTest extends ElasticsearchBackendGeneratedRequestTestBase {
    private final String stream1Id = "stream1Id";
    private final String stream2Id = "stream2Id";

    @Before
    public void setUp() throws Exception {
        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("successfulMultiSearchResponse.json")));
        when(indexLookup.indexNamesForStreamsInTimeRange(eq(ImmutableSet.of(stream1Id)), any()))
                .thenReturn(ImmutableSet.of("index1", "index2"));
        when(indexLookup.indexNamesForStreamsInTimeRange(eq(ImmutableSet.of(stream2Id)), any()))
                .thenReturn(ImmutableSet.of("index3"));
    }

    @Test
    public void searchTypeWithEmptyStreamsDefaultsToQueriesStreams() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .streams(Collections.emptySet())
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index1,index2");
    }

    @Test
    public void searchTypeWithoutStreamsDefaultsToQueriesStreams() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index1,index2");
    }

    @Test
    public void searchTypeWithStreamsOverridesQueriesStreams() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .streams(Collections.singleton(stream2Id))
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index3");
    }

    @Test
    public void queryWithMixedPresenceOfOverridesIncludesMultipleSetsOfIndices() throws IOException {
        final Query query = queryFor(Pivot.builder()
                                .id("pivot1")
                                .series(Collections.singletonList(Average.builder().field("field1").build()))
                                .rollup(true)
                                .streams(Collections.singleton(stream2Id))
                                .build(),
                        Pivot.builder()
                                .id("pivot2")
                                .series(Collections.singletonList(Max.builder().field("field2").build()))
                                .rollup(true)
                                .streams(Collections.emptySet())
                                .build());

        final MultiSearch request = run(query);
        assertThat(indicesOf(request).get(0)).isEqualTo("index3");
        assertThat(indicesOf(request).get(1)).isEqualTo("index1,index2");
    }

    private Query queryFor(SearchType... searchTypes) {
        return Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .timerange(timeRangeForTest())
                .filter(StreamFilter.ofId(stream1Id))
                .searchTypes(Arrays.stream(searchTypes).collect(Collectors.toSet()))
                .build();
    }

    private MultiSearch run(Query query) throws IOException {
        final SearchJob job = searchJobForQuery(query);
        final ESGeneratedQueryContext context = this.elasticsearchBackend.generate(job, query, Collections.emptySet());

        this.elasticsearchBackend.doRun(job, query, context, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        return clientRequestCaptor.getValue();
    }
}
