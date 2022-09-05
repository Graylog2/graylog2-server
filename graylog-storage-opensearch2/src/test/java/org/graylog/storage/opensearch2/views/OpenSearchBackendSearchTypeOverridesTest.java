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
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.storage.opensearch2.testing.TestMultisearchResponse;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.opensearch.action.search.MultiSearchResponse;
import org.opensearch.action.search.SearchRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.storage.opensearch2.views.ViewsUtils.indicesOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OpenSearchBackendSearchTypeOverridesTest extends OpenSearchBackendGeneratedRequestTestBase {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SearchJob searchJob;
    private Query query;

    @Before
    public void setUpFixtures() throws InvalidRangeParametersException {
        final Set<SearchType> searchTypes = new HashSet<>() {{
            add(
                    Pivot.builder()
                            .id("pivot1")
                            .series(Collections.singletonList(Average.builder().field("field1").build()))
                            .rollup(true)
                            .timerange(DerivedTimeRange.of(AbsoluteRange.create("2019-09-11T10:31:52.819Z", "2019-09-11T10:36:52.823Z")))
                            .filters(Collections.singletonList(InlineQueryStringSearchFilter.builder().title("Pivot1 local filter").description("Pivot1 local filter").queryString("local:filter").build()))
                            .build()
            );
            add(
                    Pivot.builder()
                            .id("pivot2")
                            .series(Collections.singletonList(Max.builder().field("field2").build()))
                            .rollup(true)
                            .query(ElasticsearchQueryString.of("source:babbage"))
                            .build()
            );
        }};
        this.query = Query.builder()
                .id("query1")
                .searchTypes(searchTypes)
                .query(ElasticsearchQueryString.of("production:true"))
                .filter(StreamFilter.ofId("stream1"))
                .filters(Collections.singletonList(InlineQueryStringSearchFilter.builder().title("Global filter").description("Global filter").queryString("global:filter").build()))
                .timerange(timeRangeForTest())
                .build();

        this.searchJob = searchJobForQuery(this.query);
    }

    @Test
    public void overridesInSearchTypeAreIncorporatedIntoGeneratedQueries() throws IOException {
        final OSGeneratedQueryContext queryContext = this.openSearchBackend.generate(searchJob, query, Collections.emptySet());
        final MultiSearchResponse response = TestMultisearchResponse.fromFixture("successfulMultiSearchResponse.json");
        final List<MultiSearchResponse.Item> items = Arrays.stream(response.getResponses())
                .collect(Collectors.toList());
        when(client.msearch(any(), any())).thenReturn(items);

        final List<SearchRequest> generatedRequest = run(searchJob, query, queryContext, Collections.emptySet());

        final DocumentContext pivot1 = parse(generatedRequest.get(0).source().toString());
        final DocumentContext pivot2 = parse(generatedRequest.get(1).source().toString());

        assertThat(queryStrings(pivot1)).containsExactly("production:true", "global:filter", "local:filter");
        assertThat(timerangeFrom(pivot1)).containsExactly("2019-09-11 10:31:52.819");
        assertThat(timerangeTo(pivot1)).containsExactly("2019-09-11 10:36:52.823");
        assertThat(streams(pivot1)).containsExactly(Collections.singletonList("stream1"));

        assertThat(queryStrings(pivot2)).containsExactly("production:true", "global:filter", "source:babbage");
        assertThat(timerangeFrom(pivot2)).containsExactly("2018-08-23 08:02:00.247");
        assertThat(timerangeTo(pivot2)).containsExactly("2018-08-23 08:07:00.252");
        assertThat(streams(pivot2)).containsExactly(Collections.singletonList("stream1"));
    }

    private DocumentContext parse(String json) {
        return JsonPath
                .using(Configuration.builder()
                        .mappingProvider(new JacksonMappingProvider())
                        .build())
                .parse(json);
    }

    private List<String> queryStrings(DocumentContext pivot) {
        return pivot.read("$..query_string.query", new TypeRef<>() {});
    }

    private List<List<String>> streams(DocumentContext pivot) {
        return pivot.read("$..terms.streams", new TypeRef<>() {});
    }

    private List<String> timerangeFrom(DocumentContext pivot) {
        return pivot.read("$..timestamp.from", new TypeRef<>() {});
    }

    private List<String> timerangeTo(DocumentContext pivot) {
        return pivot.read("$..timestamp.to", new TypeRef<>() {});
    }

    @Test
    public void timerangeOverridesAffectIndicesSelection() throws IOException, InvalidRangeParametersException {
        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("stream1"), timeRangeForTest()))
                .thenReturn(ImmutableSet.of("queryIndex"));

        TimeRange tr = AbsoluteRange.create("2019-09-11T10:31:52.819Z", "2019-09-11T10:36:52.823Z");
        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("stream1"), tr))
                .thenReturn(ImmutableSet.of("searchTypeIndex"));

        final OSGeneratedQueryContext queryContext = this.openSearchBackend.generate(searchJob, query, Collections.emptySet());
        final MultiSearchResponse response = TestMultisearchResponse.fromFixture("successfulMultiSearchResponse.json");
        final List<MultiSearchResponse.Item> items = Arrays.stream(response.getResponses())
                .collect(Collectors.toList());
        when(client.msearch(any(), any())).thenReturn(items);

        final List<SearchRequest> generatedRequest = run(searchJob, query, queryContext, Collections.emptySet());

        assertThat(indicesOf(generatedRequest))
                .hasSize(2)
                .containsExactly(
                        "searchTypeIndex",
                        "queryIndex"
                );
    }
}
