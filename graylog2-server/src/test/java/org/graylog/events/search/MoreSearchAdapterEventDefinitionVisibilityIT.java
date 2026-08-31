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

import org.graylog.plugins.views.search.searchtypes.pivot.buckets.NumberRange;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.resources.entities.Slice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the storage adapters honour {@link EventDefinitionFilter}, which is what backs the
 * {@code enforce_event_definition_permissions} setting. The setting decides which filter a subject gets
 * (see {@link EventDefinitionFilterFactory}); these tests assert that each search engine actually applies
 * the filter it is handed, on every read path that takes one.
 * <p>
 * The fixture holds five events across three event definitions, spread over two source streams:
 * <pre>
 *   def-aaa: edv-a1 (stream-a, risk_score 10), edv-a2 (stream-b, risk_score 20)
 *   def-bbb: edv-b1 (stream-a, risk_score 30), edv-b2 (stream-b, risk_score 40)
 *   def-ccc: edv-c1 (stream-a, risk_score 50)
 * </pre>
 */
public abstract class MoreSearchAdapterEventDefinitionVisibilityIT extends ElasticsearchBaseTest {

    private static final String INDEX_NAME = "graylog_0";
    private static final Set<String> EVENT_STREAMS = Set.of("000000000000000000000002");

    private static final String DEF_A = "def-aaa";
    private static final String DEF_B = "def-bbb";
    private static final String DEF_C = "def-ccc";

    private static final AbsoluteRange TIMERANGE =
            AbsoluteRange.create("2025-01-15 00:00:00.000", "2025-01-16 00:00:00.000");

    private MoreSearchAdapter adapter;

    @BeforeEach
    public void setUp() {
        adapter = createMoreSearchAdapter();
        importFixture("org/graylog/events/search/more_search_adapter_event_definition_visibility.json");
    }

    protected abstract MoreSearchAdapter createMoreSearchAdapter();

    // --- eventSearch ---

    @Test
    public void eventSearchReturnsEveryEventWhenAllDefinitionsAreAllowed() {
        assertThat(definitionIdsOf(eventSearch(EventDefinitionFilter.allAllowed())))
                .containsExactlyInAnyOrder(DEF_A, DEF_A, DEF_B, DEF_B, DEF_C);
    }

    @Test
    public void eventSearchOnlyReturnsEventsFromAllowedDefinitions() {
        assertThat(definitionIdsOf(eventSearch(EventDefinitionFilter.allowList(Set.of(DEF_A)))))
                .containsExactlyInAnyOrder(DEF_A, DEF_A);
    }

    @Test
    public void eventSearchReturnsEventsFromEveryAllowedDefinition() {
        assertThat(definitionIdsOf(eventSearch(EventDefinitionFilter.allowList(Set.of(DEF_A, DEF_C)))))
                .containsExactlyInAnyOrder(DEF_A, DEF_A, DEF_C);
    }

    /**
     * The empty allow-list is the case that matters most: a user permitted to read no event definition at
     * all must see nothing, rather than the filter degrading into a no-op.
     */
    @Test
    public void eventSearchReturnsNothingForAnEmptyAllowList() {
        assertThat(eventSearch(EventDefinitionFilter.allowList(Set.of())).results()).isEmpty();
    }

    @Test
    public void eventSearchReturnsNothingForAnAllowListOfUnknownDefinitions() {
        assertThat(eventSearch(EventDefinitionFilter.allowList(Set.of("def-does-not-exist"))).results()).isEmpty();
    }

    /**
     * The two permission filters must intersect, not union: an event has to clear both the source stream
     * allow-list and the event definition allow-list to be returned.
     */
    @Test
    public void eventSearchIntersectsDefinitionAndSourceStreamFilters() {
        final MoreSearch.Result result = adapter.eventSearch("", TIMERANGE, Set.of(INDEX_NAME), Sorting.DEFAULT,
                1, 10, EVENT_STREAMS, "", SourceStreamFilter.allowList(Set.of("stream-a")),
                EventDefinitionFilter.allowList(Set.of(DEF_A, DEF_B)), Map.of());

        // def-aaa and def-bbb each have one event in stream-a and one in stream-b; only the stream-a ones may show.
        assertThat(keysOf(result)).containsExactlyInAnyOrder("edv-a1", "edv-b1");
    }

    @Test
    public void eventSearchAppliesTheDefinitionFilterAlongsideAQueryString() {
        final MoreSearch.Result result = adapter.eventSearch("source:test-host", TIMERANGE, Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, EVENT_STREAMS, "", SourceStreamFilter.allAllowed(),
                EventDefinitionFilter.allowList(Set.of(DEF_C)), Map.of());

        assertThat(keysOf(result)).containsExactly("edv-c1");
    }

    // --- eventHistogram ---

    @Test
    public void eventHistogramCountsOnlyEventsFromAllowedDefinitions() {
        assertThat(histogramTotal(EventDefinitionFilter.allAllowed())).isEqualTo(5);
        assertThat(histogramTotal(EventDefinitionFilter.allowList(Set.of(DEF_A)))).isEqualTo(2);
        assertThat(histogramTotal(EventDefinitionFilter.allowList(Set.of(DEF_A, DEF_B)))).isEqualTo(4);
    }

    @Test
    public void eventHistogramIsEmptyForAnEmptyAllowList() {
        assertThat(histogramTotal(EventDefinitionFilter.allowList(Set.of()))).isZero();
    }

    // --- aggregateSlicesForColumn ---

    @Test
    public void aggregateSlicesForColumnOnlyCountsEventsFromAllowedDefinitions() {
        assertThat(sliceCounts(slicesForColumn(EventDefinitionFilter.allAllowed(), "event_definition_id")))
                .containsExactlyInAnyOrderEntriesOf(Map.of(DEF_A, 2, DEF_B, 2, DEF_C, 1));

        assertThat(sliceCounts(slicesForColumn(EventDefinitionFilter.allowList(Set.of(DEF_A, DEF_C)), "event_definition_id")))
                .containsExactlyInAnyOrderEntriesOf(Map.of(DEF_A, 2, DEF_C, 1));
    }

    /**
     * The slice values themselves must not leak: a definition the subject may not read may not appear as a
     * bucket key at all, not even with a count of zero.
     */
    @Test
    public void aggregateSlicesForColumnDoesNotEmitBucketsForDisallowedDefinitions() {
        assertThat(sliceCounts(slicesForColumn(EventDefinitionFilter.allowList(Set.of(DEF_A)), "event_definition_id")))
                .containsOnlyKeys(DEF_A);
    }

    @Test
    public void aggregateSlicesForColumnFiltersValuesOfOtherColumnsToo() {
        // The tags of def-bbb and def-ccc events must not surface for a subject that may only read def-aaa.
        assertThat(sliceCounts(slicesForColumn(EventDefinitionFilter.allowList(Set.of(DEF_A)), "tags")))
                .containsExactlyInAnyOrderEntriesOf(Map.of("windows", 2));
    }

    @Test
    public void aggregateSlicesForColumnReturnsNothingForAnEmptyAllowList() {
        assertThat(slicesForColumn(EventDefinitionFilter.allowList(Set.of()), "event_definition_id")).isEmpty();
    }

    // --- aggregateSlicesForRangeQuery ---

    @Test
    public void aggregateSlicesForRangeQueryOnlyCountsEventsFromAllowedDefinitions() {
        final List<NumberRange> ranges = List.of(new NumberRange(0.0, 25.0), new NumberRange(25.0, 100.0));

        // All definitions: risks 10 and 20 fall in the low bucket, 30/40/50 in the high one.
        assertThat(totalCount(slicesForRangeQuery(EventDefinitionFilter.allAllowed(), ranges))).isEqualTo(5);

        // def-aaa only: risks 10 and 20, so nothing lands in the high bucket.
        assertThat(totalCount(slicesForRangeQuery(EventDefinitionFilter.allowList(Set.of(DEF_A)), ranges))).isEqualTo(2);
    }

    @Test
    public void aggregateSlicesForRangeQueryReturnsNothingForAnEmptyAllowList() {
        final List<NumberRange> ranges = List.of(new NumberRange(0.0, 100.0));

        assertThat(totalCount(slicesForRangeQuery(EventDefinitionFilter.allowList(Set.of()), ranges))).isZero();
    }

    // --- helpers ---

    private MoreSearch.Result eventSearch(EventDefinitionFilter eventDefinitionFilter) {
        return adapter.eventSearch("", TIMERANGE, Set.of(INDEX_NAME), Sorting.DEFAULT, 1, 10, EVENT_STREAMS,
                "", SourceStreamFilter.allAllowed(), eventDefinitionFilter, Map.of());
    }

    private long histogramTotal(EventDefinitionFilter eventDefinitionFilter) {
        final MoreSearch.Histogram histogram = adapter.eventHistogram("", TIMERANGE, Set.of(INDEX_NAME),
                EVENT_STREAMS, "", SourceStreamFilter.allAllowed(), eventDefinitionFilter, ZoneId.of("UTC"), Map.of());

        return histogram.buckets().events().stream().mapToLong(MoreSearch.Histogram.Bucket::count).sum();
    }

    private List<Slice> slicesForColumn(EventDefinitionFilter eventDefinitionFilter, String column) {
        return adapter.aggregateSlicesForColumn("", TIMERANGE, Set.of(INDEX_NAME), EVENT_STREAMS, "",
                SourceStreamFilter.allAllowed(), eventDefinitionFilter, Map.of(), column, null, Map.of(), 100);
    }

    private List<Slice> slicesForRangeQuery(EventDefinitionFilter eventDefinitionFilter, List<NumberRange> ranges) {
        return adapter.aggregateSlicesForRangeQuery("", TIMERANGE, Set.of(INDEX_NAME), EVENT_STREAMS, "",
                SourceStreamFilter.allAllowed(), eventDefinitionFilter, Map.of(), "risk_score",
                Map.of(), ranges);
    }

    private static List<String> definitionIdsOf(MoreSearch.Result result) {
        return fieldOf(result, "event_definition_id");
    }

    private static List<String> keysOf(MoreSearch.Result result) {
        return fieldOf(result, "event_key");
    }

    private static List<String> fieldOf(MoreSearch.Result result, String field) {
        return result.results().stream()
                .map(ResultMessage::getMessage)
                .map(message -> (String) message.getField(field))
                .collect(Collectors.toList());
    }

    private static Map<String, Integer> sliceCounts(List<Slice> slices) {
        return slices.stream()
                .filter(slice -> slice.count() > 0)
                .collect(Collectors.toMap(Slice::value, Slice::count));
    }

    private static int totalCount(List<Slice> slices) {
        return slices.stream().mapToInt(Slice::count).sum();
    }
}
