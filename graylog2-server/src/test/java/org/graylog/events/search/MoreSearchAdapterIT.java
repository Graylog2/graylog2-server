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
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class MoreSearchAdapterIT extends ElasticsearchBaseTest {

    private final static String INDEX_NAME = "graylog_0";
    private final static Set<String> ALL_STREAMS = Set.of("000000000000000000000001");

    private MoreSearchAdapter toTest;

    @Before
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

    @NotNull
    private MoreSearchAdapter.ScrollEventsCallback getCountingAndCollectingScrollEventsCallback(AtomicInteger count,
                                                                                                Collection<ResultMessage> allResults) {
        return (chunkResults, requestContinue) -> {
            count.incrementAndGet();
            allResults.addAll(chunkResults);
        };
    }


    private void verifyResult(MoreSearch.Result result, int resultIndex, int expectedMessageNumber) {
        assertThat(result.results().get(resultIndex))
                .satisfies(rm -> {
                    assertThat(rm.getMessage().getField("number"))
                            .isNotNull()
                            .isEqualTo(expectedMessageNumber);
                });
    }

    private void verifyResult(List<ResultMessage> result, int resultIndex, int expectedMessageNumber) {
        assertThat(result)
                .element(resultIndex)
                .satisfies(rm -> {
                    assertThat(rm.getMessage().getField("number"))
                            .isNotNull()
                            .isEqualTo(expectedMessageNumber);
                });
    }
}
