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

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class MoreSearchAdapterIT extends ElasticsearchBaseTest {

    private final static String INDEX_NAME = "graylog_0";
    private final static Set<String> ALL_STREAMS = Set.of("000000000000000000000001");

    private MoreSearchAdapter toTest;

    @BeforeEach
    public void setUp() throws Exception {
        toTest = createMoreSearchAdapter();
        importFixture("org/graylog/events/search/more_search_adapter.json");
    }

    protected abstract MoreSearchAdapter createMoreSearchAdapter();

    @Test
    public void eventSearchGetsAllMessages() {
        int expectedNumberOfMessages = 7;

        final MoreSearch.Result result = toTest.eventSearch("*", RelativeRange.allTime(), Set.of(INDEX_NAME), Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of());
        assertThat(result.results()).hasSize(expectedNumberOfMessages);
        for (int i = 0; i < expectedNumberOfMessages; i++) {
            verifyResult(result, i, expectedNumberOfMessages - i);
        }
    }

    @Test
    public void eventsHistogram() {
        final MoreSearch.Histogram result = toTest.eventHistogram("*", AbsoluteRange.create("2015-01-01 01:00:00.000", "2022-01-01 01:00:00.000"), Set.of(INDEX_NAME), ALL_STREAMS,"*", Set.of(), ZoneId.of("Europe/Vienna"), Map.of());
        Assertions.assertThat(result.buckets().alerts())
                .hasSize(85);
        Assertions.assertThat(result.buckets().events())
                .hasSize(85);

        final List<MoreSearch.Histogram.Bucket> alerts = result.buckets().alerts().stream().filter(b -> b.count() > 0).toList();
        Assertions.assertThat(alerts)
                .extracting(a -> a.startDate().getYear())
                .contains(2015, 2017, 2021);

        final List<MoreSearch.Histogram.Bucket> events = result.buckets().events().stream().filter(b -> b.count() > 0).toList();
        Assertions.assertThat(events)
                .extracting(a -> a.startDate().getYear())
                .contains(2016, 2018, 2019, 2020);
    }


    @Test
    public void eventSearchGetsPaginatedMessages() {
        //page 1 : 7,6, page 2 : 5, 4, page 3: 3,2, page 4 : 1
        for (int i = 0; i < 3; i++) {
            final MoreSearch.Result result = toTest.eventSearch("*", RelativeRange.allTime(), Set.of(INDEX_NAME), Sorting.DEFAULT, i + 1, 2, ALL_STREAMS, "", Set.of());
            assertThat(result.results()).hasSize(2);
            verifyResult(result, 0, 7 - i * 2);
            verifyResult(result, 1, 6 - i * 2);
        }

        final MoreSearch.Result result = toTest.eventSearch("*", RelativeRange.allTime(), Set.of(INDEX_NAME), Sorting.DEFAULT, 4, 2, ALL_STREAMS, "", Set.of());
        assertThat(result.results()).hasSize(1);
        verifyResult(result, 0, 1);

    }

    @Test
    public void eventSearchReturnsNoMessagesIfTheyDoNotMatchQueryString() {
        final MoreSearch.Result result = toTest.eventSearch("message:moin", RelativeRange.allTime(), Set.of(INDEX_NAME), Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of());
        assertThat(result.results()).isEmpty();
    }

    @Test
    public void eventSearchNoExceptionIfIndexUnavailable() {
        final MoreSearch.Result result = toTest.eventSearch("*", RelativeRange.allTime(), Set.of("unavailable"), Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of());
        assertThat(result.results()).isEmpty();
    }

    @Test
    public void eventSearchPartiallyAvailable() {
        int expectedNumberOfMessages = 7;

        final MoreSearch.Result result = toTest.eventSearch("*", RelativeRange.allTime(), Set.of("unavailable", INDEX_NAME), Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of());
        assertThat(result.results()).hasSize(expectedNumberOfMessages);
        for (int i = 0; i < expectedNumberOfMessages; i++) {
            verifyResult(result, i, expectedNumberOfMessages - i);
        }
    }

    @Test
    public void scrollEventsReturnsAllMessages() throws Exception {
        int expectedNumberOfMessages = 7;
        int batchSize = 2;
        final AtomicInteger count = new AtomicInteger(0);
        final List<ResultMessage> allResults = new ArrayList<>(expectedNumberOfMessages);

        toTest.scrollEvents("*", RelativeRange.allTime(), Set.of(INDEX_NAME), ALL_STREAMS, Collections.emptyList(), batchSize,
                getCountingAndCollectingScrollEventsCallback(count, allResults));

        assertThat(count).hasValue(expectedNumberOfMessages / batchSize + 1);

        assertThat(allResults)
                .hasSize(expectedNumberOfMessages);
        for (int i = 0; i < expectedNumberOfMessages; i++) {
            verifyResult(allResults, i, i + 1);
        }
    }

    @Test
    public void scrollEventsReturnsMessagesMatchingTimeRange() throws Exception {
        int expectedNumberOfMessages = 4; //2015,2016 and 2017 messages get excluded
        int batchSize = 2;
        final AtomicInteger count = new AtomicInteger(0);
        final List<ResultMessage> allResults = new ArrayList<>(expectedNumberOfMessages);

        toTest.scrollEvents("*", AbsoluteRange.create(new DateTime(2018, 1, 1, 1, 1, DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC)), Set.of(INDEX_NAME), Set.of("000000000000000000000001"), Collections.emptyList(), batchSize,
                getCountingAndCollectingScrollEventsCallback(count, allResults));

        assertThat(count).hasValue(expectedNumberOfMessages / batchSize);

        assertThat(allResults)
                .hasSize(expectedNumberOfMessages);

        for (int i = 0; i < expectedNumberOfMessages; i++) {
            verifyResult(allResults, i, i + 4);
        }
    }

    @Test
    public void scrollEventsReturnsMessagesMatchingQueryString() throws Exception {
        int expectedNumberOfMessages = 3; //3 messages have "Ahoj" present
        int batchSize = 2;
        final AtomicInteger count = new AtomicInteger(0);
        final List<ResultMessage> allResults = new ArrayList<>(expectedNumberOfMessages);

        toTest.scrollEvents("message:Ahoj", RelativeRange.allTime(), Set.of(INDEX_NAME), Set.of("000000000000000000000001"), Collections.emptyList(), batchSize,
                getCountingAndCollectingScrollEventsCallback(count, allResults));

        assertThat(count).hasValue(2);

        assertThat(allResults)
                .hasSize(expectedNumberOfMessages);
        for (int i = 0; i < expectedNumberOfMessages; i++) {
            verifyResult(allResults, i, i + 5);
        }
    }

    @Test
    public void scrollEventsReturnsMessagesMatchingQueryStringAndTimeRange() throws Exception {
        int expectedNumberOfMessages = 1;
        int batchSize = 2;
        final AtomicInteger count = new AtomicInteger(0);
        final List<ResultMessage> allResults = new ArrayList<>(expectedNumberOfMessages);

        toTest.scrollEvents("message:Ahoj", AbsoluteRange.create(new DateTime(2021, 1, 1, 1, 1, DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC)), Set.of(INDEX_NAME), Set.of("000000000000000000000001"), Collections.emptyList(), batchSize,
                getCountingAndCollectingScrollEventsCallback(count, allResults));

        assertThat(count).hasValue(1);

        assertThat(allResults)
                .hasSize(expectedNumberOfMessages);
        verifyResult(allResults, 0, 7);
    }

    @Test
    public void scrollEventsReturnsNoMessagesIfTheyDoNotMatchQueryString() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final Collection<ResultMessage> allResults = new ArrayList<>(1);
        toTest.scrollEvents("message:moin", RelativeRange.allTime(), Set.of(INDEX_NAME), Set.of("000000000000000000000001"), Collections.emptyList(), 2,
                getCountingAndCollectingScrollEventsCallback(count, allResults));

        assertThat(count).hasValue(0);

        assertThat(allResults).isEmpty();
    }

    @Test
    public void scrollEventsNoExceptionIfIndexUnavailable() throws Exception {
        int expectedNumberOfMessages = 7;
        int batchSize = 2;
        final AtomicInteger count = new AtomicInteger(0);
        final List<ResultMessage> allResults = new ArrayList<>(expectedNumberOfMessages);

        toTest.scrollEvents("*", RelativeRange.allTime(), Set.of("unavailable"), ALL_STREAMS, Collections.emptyList(), batchSize,
                getCountingAndCollectingScrollEventsCallback(count, allResults));
        assertThat(allResults).isEmpty();
    }

    @Test
    public void eventSearchSortsCorrectlyByTimestamp() {
        // Test ascending order (default)
        final MoreSearch.Result resultAsc = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT,
                1, 10, ALL_STREAMS, "", Set.of());

        assertThat(resultAsc.results()).hasSize(7);
        // Default sorting is descending by timestamp, so newest first
        verifyResult(resultAsc, 0, 7); // newest first
        verifyResult(resultAsc, 6, 1); // oldest last
    }

    @Test
    public void eventSearchSortsCorrectlyAscending() {
        // Test explicit ascending order
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                new Sorting("timestamp", Sorting.Direction.ASC),
                1, 10, ALL_STREAMS, "", Set.of());

        assertThat(result.results()).hasSize(7);
        // Ascending order - oldest first
        verifyResult(result, 0, 1); // oldest first
        verifyResult(result, 6, 7); // newest last
    }

    @Test
    public void eventSearchSortsCorrectlyDescending() {
        // Test explicit descending order
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                new Sorting("timestamp", Sorting.Direction.DESC),
                1, 10, ALL_STREAMS, "", Set.of());

        assertThat(result.results()).hasSize(7);
        // Descending order - newest first
        verifyResult(result, 0, 7); // newest first
        verifyResult(result, 6, 1); // oldest last
    }

    @Test
    public void eventSearchWithExtraFiltersGreaterThan() {
        final Map<String, Set<String>> extraFilters = Map.of("number", Set.of(">5"));
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of(), extraFilters);

        assertThat(result.results()).hasSize(2);
        verifyResult(result, 0, 7);
        verifyResult(result, 1, 6);
    }

    @Test
    public void eventSearchWithExtraFiltersGreaterThanOrEqual() {
        final Map<String, Set<String>> extraFilters = Map.of("number", Set.of(">=6"));
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of(), extraFilters);

        assertThat(result.results()).hasSize(2);
        verifyResult(result, 0, 7);
        verifyResult(result, 1, 6);
    }

    @Test
    public void eventSearchWithExtraFiltersLessThan() {
        final Map<String, Set<String>> extraFilters = Map.of("number", Set.of("<4"));
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of(), extraFilters);

        assertThat(result.results()).hasSize(3);
        verifyResult(result, 0, 3);
        verifyResult(result, 1, 2);
        verifyResult(result, 2, 1);
    }

    @Test
    public void eventSearchWithExtraFiltersLessThanOrEqual() {
        final Map<String, Set<String>> extraFilters = Map.of("number", Set.of("<=3"));
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of(), extraFilters);

        assertThat(result.results()).hasSize(3);
        verifyResult(result, 0, 3);
        verifyResult(result, 1, 2);
        verifyResult(result, 2, 1);
    }

    @Test
    public void eventSearchWithMultipleExtraFilters() {
        final Map<String, Set<String>> extraFilters = Map.of(
                "number", Set.of(">=4", "<=6")
        );
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of(), extraFilters);

        assertThat(result.results()).hasSize(3);
        verifyResult(result, 0, 6);
        verifyResult(result, 1, 5);
        verifyResult(result, 2, 4);
    }

    @Test
    public void eventSearchWithFilterString() {
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "alert:true", Set.of());

        assertThat(result.results()).hasSize(3); // Only messages with alert:true
        verifyResult(result, 0, 7);
        verifyResult(result, 1, 3);
        verifyResult(result, 2, 1);
    }

    @Test
    public void eventSearchWithBothQueryAndFilter() {
        final MoreSearch.Result result = toTest.eventSearch("message:Ahoj",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "alert:true", Set.of());

        assertThat(result.results()).hasSize(1); // Only message 7 has both
        verifyResult(result, 0, 7);
    }

    @Test
    public void eventSearchWithQueryAndExtraFilters() {
        final Map<String, Set<String>> extraFilters = Map.of("number", Set.of(">4"));
        final MoreSearch.Result result = toTest.eventSearch("message:Ahoj",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "", Set.of(), extraFilters);

        assertThat(result.results()).hasSize(3); // Messages 5, 6, 7 have "Ahoj" and number > 4
        verifyResult(result, 0, 7);
        verifyResult(result, 1, 6);
        verifyResult(result, 2, 5);
    }

    @Test
    public void eventSearchReturnsEmptyForPageBeyondResults() {
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 100, 10, ALL_STREAMS, "", Set.of());

        assertThat(result.results()).isEmpty();
    }

    @Test
    public void scrollEventsCanBeStoppedEarly() throws Exception {
        int batchSize = 2;
        final AtomicInteger batchCount = new AtomicInteger(0);
        final List<ResultMessage> allResults = new ArrayList<>();

        toTest.scrollEvents("*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                ALL_STREAMS, Collections.emptyList(), batchSize,
                (chunkResults, requestContinue) -> {
                    batchCount.incrementAndGet();
                    allResults.addAll(chunkResults);
                    if (batchCount.get() >= 2) {
                        requestContinue.set(false); // Stop after 2 batches
                    }
                });

        assertThat(batchCount.get()).isEqualTo(2);
        assertThat(allResults).hasSizeLessThanOrEqualTo(4); // 2 batches * 2 per batch
    }

    @Test
    public void scrollEventsWithDifferentBatchSizes() throws Exception {
        // Test with batch size of 1
        int batchSize = 1;
        final AtomicInteger count = new AtomicInteger(0);
        final List<ResultMessage> allResults = new ArrayList<>();

        toTest.scrollEvents("*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                ALL_STREAMS, Collections.emptyList(), batchSize,
                getCountingAndCollectingScrollEventsCallback(count, allResults));

        assertThat(count).hasValue(7); // Should have 7 batches with 1 message each
        assertThat(allResults).hasSize(7);

        // Test with larger batch size
        final AtomicInteger count2 = new AtomicInteger(0);
        final List<ResultMessage> allResults2 = new ArrayList<>();
        int batchSize2 = 10;

        toTest.scrollEvents("*", RelativeRange.allTime(), Set.of(INDEX_NAME),
                ALL_STREAMS, Collections.emptyList(), batchSize2,
                getCountingAndCollectingScrollEventsCallback(count2, allResults2));

        assertThat(count2).hasValue(1); // Should have 1 batch with all 7 messages
        assertThat(allResults2).hasSize(7);
    }

    @Test
    public void eventSearchExcludesForbiddenSourceStreams() {
        // Message 6 has "forbidden_stream" in source_streams, so it should be excluded
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "",
                Set.of("forbidden_stream"));

        assertThat(result.results()).hasSize(6); // Should exclude message 6
        // Verify message 6 is not in results
        assertThat(result.results())
                .noneMatch(rm -> rm.getMessage().getField("number").equals(6));
    }

    @Test
    public void eventSearchExcludesMultipleForbiddenSourceStreams() {
        // Messages 5 and 6 have source_streams, should be excluded
        final MoreSearch.Result result = toTest.eventSearch("*",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "",
                Set.of("forbidden_stream", "source_stream_a"));

        assertThat(result.results()).hasSize(5); // Should exclude messages 5 and 6
        // Verify messages 5 and 6 are not in results
        assertThat(result.results())
                .noneMatch(rm -> {
                    Object number = rm.getMessage().getField("number");
                    return number.equals(5) || number.equals(6);
                });
    }

    @Test
    public void eventSearchWithForbiddenStreamsAndQueryString() {
        // Search for "Ahoj" messages (5, 6, 7) but exclude those with forbidden stream (6)
        final MoreSearch.Result result = toTest.eventSearch("message:Ahoj",
                RelativeRange.allTime(),
                Set.of(INDEX_NAME),
                Sorting.DEFAULT, 1, 10, ALL_STREAMS, "",
                Set.of("forbidden_stream"));

        assertThat(result.results()).hasSize(2); // Should get messages 5 and 7, but not 6
        verifyResult(result, 0, 7);
        verifyResult(result, 1, 5);
    }

    @Nonnull
    private MoreSearchAdapter.ScrollEventsCallback getCountingAndCollectingScrollEventsCallback(AtomicInteger count,
                                                                                                Collection<ResultMessage> allResults) {
        return (chunkResults, requestContinue) -> {
            count.incrementAndGet();
            allResults.addAll(chunkResults);
        };
    }


    private void verifyResult(MoreSearch.Result result, int resultIndex, int expectedMessageNumber) {
        assertThat(result.results().get(resultIndex))
                .satisfies(rm ->
                    assertThat(rm.getMessage().getField("number"))
                            .isNotNull()
                            .isEqualTo(expectedMessageNumber));
    }

    private void verifyResult(List<ResultMessage> result, int resultIndex, int expectedMessageNumber) {
        assertThat(result)
                .element(resultIndex)
                .satisfies(rm ->
                    assertThat(rm.getMessage().getField("number"))
                            .isNotNull()
                            .isEqualTo(expectedMessageNumber));
    }
}
