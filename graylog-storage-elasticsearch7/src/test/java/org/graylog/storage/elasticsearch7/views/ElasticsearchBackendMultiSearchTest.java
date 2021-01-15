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
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.storage.elasticsearch7.testing.TestMultisearchResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendMultiSearchTest extends ElasticsearchBackendGeneratedRequestTestBase {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SearchJob searchJob;
    private Query query;

    @Before
    public void setUpFixtures() {
        final Set<SearchType> searchTypes = new HashSet<SearchType>() {{
            add(
                    Pivot.builder()
                            .id("pivot1")
                            .series(Collections.singletonList(Average.builder().field("field1").build()))
                            .rollup(true)
                            .build()
            );
            add(
                    Pivot.builder()
                            .id("pivot2")
                            .series(Collections.singletonList(Max.builder().field("field2").build()))
                            .rollup(true)
                            .build()
            );
        }};
        this.query = Query.builder()
                .id("query1")
                .searchTypes(searchTypes)
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .timerange(timeRangeForTest())
                .build();

        this.searchJob = searchJobForQuery(this.query);
    }

    @Test
    public void everySearchTypeGeneratesASearchSourceBuilder() {
        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());

        assertThat(queryContext.searchTypeQueries())
                .hasSize(2)
                .containsOnlyKeys("pivot1", "pivot2");
    }

    @Test
    public void everySearchTypeGeneratesOneESQuery() throws Exception {
        final MultiSearchResponse response = TestMultisearchResponse.fromFixture("successfulMultiSearchResponse.json");
        final List<MultiSearchResponse.Item> items = Arrays.stream(response.getResponses())
                .collect(Collectors.toList());
        when(client.msearch(any(), any())).thenReturn(items);

        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());
        final List<SearchRequest> generatedRequest = run(searchJob, query, queryContext, Collections.emptySet());

        assertThat(generatedRequest).hasSize(2);
    }

    @Test
    public void multiSearchResultsAreAssignedToSearchTypes() throws Exception {
        final MultiSearchResponse response = TestMultisearchResponse.fromFixture("successfulMultiSearchResponse.json");
        final List<MultiSearchResponse.Item> items = Arrays.stream(response.getResponses())
                .collect(Collectors.toList());
        when(client.msearch(any(), any())).thenReturn(items);

        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());
        final QueryResult queryResult = this.elasticsearchBackend.doRun(searchJob, query, queryContext, Collections.emptySet());

        assertThat(queryResult.searchTypes()).containsOnlyKeys("pivot1", "pivot2");

        final PivotResult pivot1Result = (PivotResult) queryResult.searchTypes().get("pivot1");
        assertThat(pivot1Result.rows().get(0)).isEqualTo(
                PivotResult.Row.builder().key(ImmutableList.of()).source("leaf").addValue(
                        PivotResult.Value.create(Collections.singletonList("avg(field1)"), 27220.273504273504, true, "row-leaf")
                ).build()
        );

        final PivotResult pivot2Result = (PivotResult) queryResult.searchTypes().get("pivot2");
        assertThat(pivot2Result.rows().get(0)).isEqualTo(
                PivotResult.Row.builder().key(ImmutableList.of()).source("leaf").addValue(
                        PivotResult.Value.create(Collections.singletonList("max(field2)"), 42.0, true, "row-leaf")
                ).build()
        );
    }

    @Test
    public void oneFailingSearchTypeReturnsPartialResults() throws Exception {
        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());

        final MultiSearchResponse response = TestMultisearchResponse.fromFixture("partiallySuccessfulMultiSearchResponse.json");
        final List<MultiSearchResponse.Item> items = Arrays.stream(response.getResponses())
                .collect(Collectors.toList());
        when(client.msearch(any(), any())).thenReturn(items);

        final QueryResult queryResult = this.elasticsearchBackend.doRun(searchJob, query, queryContext, Collections.emptySet());

        assertThat(queryResult.errors()).hasSize(1);
        final SearchTypeError searchTypeError = (SearchTypeError) new ArrayList<>(queryResult.errors()).get(0);
        assertThat(searchTypeError.description()).isEqualTo(
                "Unable to perform search query: \n" +
                        "\n" +
                        "Elasticsearch exception [type=illegal_argument_exception, reason=Expected numeric type on field [field1], but got [keyword]]."
        );
        assertThat(searchTypeError.searchTypeId()).isEqualTo("pivot1");

        assertThat(queryResult.searchTypes()).containsOnlyKeys("pivot2");

        final PivotResult pivot2Result = (PivotResult) queryResult.searchTypes().get("pivot2");
        assertThat(pivot2Result.rows().get(0)).isEqualTo(
                PivotResult.Row.builder().key(ImmutableList.of()).source("leaf").addValue(
                        PivotResult.Value.create(Collections.singletonList("max(field2)"), 42.0, true, "row-leaf")
                ).build()
        );
    }
}
