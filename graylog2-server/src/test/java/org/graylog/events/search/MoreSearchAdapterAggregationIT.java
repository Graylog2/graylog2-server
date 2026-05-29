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
package org.graylog.events.search;

import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.resources.entities.Slice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.events.search.SourceStreamFilter.allAllowed;

public abstract class MoreSearchAdapterAggregationIT extends ElasticsearchBaseTest {

    private static final String INDEX_NAME = "graylog_0";
    private static final Set<String> ALL_STREAMS = Set.of("stream-a", "stream-b");

    private MoreSearchAdapter adapter;

    @BeforeEach
    public void setUp() {
        adapter = createMoreSearchAdapter();
        importFixture("org/graylog/events/search/more_search_adapter_aggregation.json");
    }

    protected abstract MoreSearchAdapter createMoreSearchAdapter();

    // --- aggregateSlicesForColumn tests ---

    @Test
    public void aggregateSlicesForColumn_groupsByField() {
        final List<Slice> result = adapter.aggregateSlicesForColumn(
                "*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                ALL_STREAMS, null, allAllowed(),
                Map.of(), "gl2_source_input", Map.of(), 100);

        final Map<String, Integer> countsByInput = result.stream()
                .collect(Collectors.toMap(Slice::value, Slice::count));

        assertThat(countsByInput)
                .containsEntry("input-1", 2)
                .containsEntry("input-2", 2)
                .containsEntry("input-3", 1);
    }

    @Test
    public void aggregateSlicesForColumn_withQueryFilter() {
        final List<Slice> result = adapter.aggregateSlicesForColumn(
                "gl2_source_input:input-1", RelativeRange.allTime(), Set.of(INDEX_NAME),
                ALL_STREAMS, null, allAllowed(),
                Map.of(), "streams", Map.of(), 100);

        final Map<String, Integer> countsByStream = result.stream()
                .collect(Collectors.toMap(Slice::value, Slice::count));

        assertThat(countsByStream)
                .hasSize(1)
                .containsEntry("stream-a", 2);
    }

    @Test
    public void aggregateSlicesForColumn_emptyResultForNoMatch() {
        final List<Slice> result = adapter.aggregateSlicesForColumn(
                "gl2_source_input:nonexistent", RelativeRange.allTime(), Set.of(INDEX_NAME),
                ALL_STREAMS, null, allAllowed(),
                Map.of(), "streams", Map.of(), 100);

        assertThat(result).isEmpty();
    }

    // --- aggregateGroupedTerms tests ---

    @Test
    public void aggregateGroupedTerms_groupsByPrimaryAndSecondary() {
        final Map<String, Map<String, Long>> result = adapter.aggregateGroupedTerms(
                "*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "gl2_source_input", "streams",
                100, 100, Set.of());

        assertThat(result).containsKey("input-1");
        assertThat(result.get("input-1")).containsEntry("stream-a", 2L);

        assertThat(result).containsKey("input-2");
        assertThat(result.get("input-2"))
                .containsEntry("stream-a", 1L)
                .containsEntry("stream-b", 2L);

        assertThat(result).containsKey("input-3");
        assertThat(result.get("input-3")).containsEntry("stream-b", 1L);
    }

    @Test
    public void aggregateGroupedTerms_withQueryFilter() {
        final Map<String, Map<String, Long>> result = adapter.aggregateGroupedTerms(
                "gl2_source_input:input-2", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "gl2_source_input", "streams",
                100, 100, Set.of());

        assertThat(result)
                .hasSize(1)
                .containsKey("input-2");
        assertThat(result.get("input-2"))
                .containsEntry("stream-a", 1L)
                .containsEntry("stream-b", 2L);
    }

    @Test
    public void aggregateGroupedTerms_emptyResultForNoMatch() {
        final Map<String, Map<String, Long>> result = adapter.aggregateGroupedTerms(
                "gl2_source_input:nonexistent", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "gl2_source_input", "streams",
                100, 100, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    public void aggregateGroupedTerms_respectsMaxBuckets() {
        final Map<String, Map<String, Long>> result = adapter.aggregateGroupedTerms(
                "*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "gl2_source_input", "streams",
                1, 100, Set.of());

        assertThat(result).hasSize(1);
    }

    // --- aggregateTerms tests ---

    @Test
    public void aggregateTerms_countsByField() {
        final Map<String, Long> result = adapter.aggregateTerms(
                "*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", 100, Set.of());

        assertThat(result)
                .containsEntry("stream-a", 3L)
                .containsEntry("stream-b", 3L);
    }

    @Test
    public void aggregateTerms_withQueryFilter() {
        final Map<String, Long> result = adapter.aggregateTerms(
                "gl2_source_input:input-2", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", 100, Set.of());

        assertThat(result)
                .containsEntry("stream-a", 1L)
                .containsEntry("stream-b", 2L);
    }

    @Test
    public void aggregateTerms_emptyResultForNoMatch() {
        final Map<String, Long> result = adapter.aggregateTerms(
                "gl2_source_input:nonexistent", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", 100, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    public void aggregateTerms_respectsMaxBuckets() {
        final Map<String, Long> result = adapter.aggregateTerms(
                "*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "gl2_source_input", 1, Set.of());

        assertThat(result).hasSize(1);
    }

    @Test
    public void aggregateTerms_countsMultiStreamDocumentsCorrectly() {
        // Doc agg-2 is in both stream-a and stream-b.
        // aggregateTerms should count it in both buckets (doc_count), unlike
        // aggregateGroupedTerms which could miss it with a same-field sub-aggregation.
        final Map<String, Long> result = adapter.aggregateTerms(
                "streams:stream-a OR streams:stream-b", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", 100, Set.of());

        assertThat(result)
                .containsEntry("stream-a", 3L)
                .containsEntry("stream-b", 3L);
    }

    // --- aggregateGroupedMetric tests ---

    @Test
    public void aggregateGroupedMetric_avgByStream() {
        final Map<String, Double> result = adapter.aggregateGroupedMetric(
                "*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", MoreSearchAdapter.AggregationType.AVG, "processing_time",
                100, Set.of());

        assertThat(result)
                .containsEntry("stream-a", 200.0)  // avg(100, 200, 300)
                .containsEntry("stream-b", 400.0);  // avg(300, 400, 500)
    }

    @Test
    public void aggregateGroupedMetric_maxByStream() {
        final Map<String, Double> result = adapter.aggregateGroupedMetric(
                "*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", MoreSearchAdapter.AggregationType.MAX, "processing_time",
                100, Set.of());

        assertThat(result)
                .containsEntry("stream-a", 300.0)
                .containsEntry("stream-b", 500.0);
    }

    @Test
    public void aggregateGroupedMetric_withQueryFilter() {
        final Map<String, Double> result = adapter.aggregateGroupedMetric(
                "gl2_source_input:input-1", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", MoreSearchAdapter.AggregationType.AVG, "processing_time",
                100, Set.of());

        assertThat(result)
                .hasSize(1)
                .containsEntry("stream-a", 150.0);  // avg(100, 200)
    }

    @Test
    public void aggregateGroupedMetric_emptyResultForNoMatch() {
        final Map<String, Double> result = adapter.aggregateGroupedMetric(
                "gl2_source_input:nonexistent", RelativeRange.allTime(), Set.of(INDEX_NAME),
                "streams", MoreSearchAdapter.AggregationType.AVG, "processing_time",
                100, Set.of());

        assertThat(result).isEmpty();
    }
}
