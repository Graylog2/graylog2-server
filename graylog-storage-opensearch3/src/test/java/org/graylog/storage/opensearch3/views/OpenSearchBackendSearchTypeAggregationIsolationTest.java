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
package org.graylog.storage.opensearch3.views;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.storage.opensearch3.testing.TestMsearchResponse;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.core.MsearchResponse;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that aggregations generated for different search types remain isolated from each other.
 * This guards against the copy of {@link MutableSearchRequestBuilder} sharing mutable state (e.g. the
 * aggregations list) between the base query and per-search-type copies.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class OpenSearchBackendSearchTypeAggregationIsolationTest extends OpenSearchBackendGeneratedRequestTestBase {

    private SearchJob searchJob;
    private Query query;

    @BeforeEach
    public void setUpFixtures() {
        final Set<SearchType> searchTypes = Set.of(
                Pivot.builder()
                        .id("pivot1")
                        .series(Collections.singletonList(Average.builder().field("field1").build()))
                        .rollup(true)
                        .build(),
                Pivot.builder()
                        .id("pivot2")
                        .series(Collections.singletonList(Max.builder().field("field2").build()))
                        .rollup(true)
                        .build()
        );
        this.query = Query.builder()
                .id("query1")
                .searchTypes(searchTypes)
                .query(ElasticsearchQueryString.of("*"))
                .timerange(timeRangeForTest())
                .build();

        this.searchJob = searchJobForQuery(this.query);
    }

    @Test
    public void searchTypeAggregationsDoNotLeakBetweenPivots() throws Exception {
        final OSGeneratedQueryContext queryContext = createContext(query);
        final MsearchResponse<JsonData> response = TestMsearchResponse.fromFixture("successfulMultiSearchResponse.json");
        mockCancellableMSearch(response);

        final List<SearchRequest> generatedRequests = run(searchJob, query, queryContext);

        assertThat(generatedRequests).hasSize(2);

        final SearchRequest pivot1Request = findRequestWithAggregationKey(generatedRequests, "pivot1-series-avg(field1)");
        final SearchRequest pivot2Request = findRequestWithAggregationKey(generatedRequests, "pivot2-series-max(field2)");

        // pivot1 should have avg aggregation but not max (besides the timestamp-max which is always present)
        assertThat(pivot1Request.aggregations()).containsKey("pivot1-series-avg(field1)");
        assertThat(pivot1Request.aggregations()).doesNotContainKey("pivot2-series-max(field2)");

        // pivot2 should have max aggregation but not avg
        assertThat(pivot2Request.aggregations()).containsKey("pivot2-series-max(field2)");
        assertThat(pivot2Request.aggregations()).doesNotContainKey("pivot1-series-avg(field1)");

        // both should have the timestamp aggregations
        assertThat(pivot1Request.aggregations()).containsKey("timestamp-min");
        assertThat(pivot1Request.aggregations()).containsKey("timestamp-max");
        assertThat(pivot2Request.aggregations()).containsKey("timestamp-min");
        assertThat(pivot2Request.aggregations()).containsKey("timestamp-max");
    }

    private SearchRequest findRequestWithAggregationKey(List<SearchRequest> requests, String aggregationKey) {
        return requests.stream()
                .filter(r -> r.aggregations().containsKey(aggregationKey))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No request found containing aggregation key: " + aggregationKey));
    }

    private OSGeneratedQueryContext createContext(Query query) {
        return this.openSearchBackend.generate(query, Collections.emptySet(), DateTimeZone.UTC);
    }
}
