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
package org.graylog2.indexer;

import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class IndexToolsAdapterIT extends ElasticsearchBaseTest {

    private static final String INDEX_NAME = "graylog_0";
    private static final String STREAM_1 = "000000000000000000000001";
    private static final String STREAM_2 = "000000000000000000000002";

    private IndexToolsAdapter adapter;

    @BeforeEach
    public void setUp() {
        client().createIndex(INDEX_NAME);
        client().waitForGreenStatus(INDEX_NAME);
        this.adapter = searchServer().adapters().indexToolsAdapter();
    }

    @Test
    public void countReturnsZeroForEmptyIndex() {
        final long count = adapter.count(Set.of(INDEX_NAME), Optional.empty());
        assertThat(count).isEqualTo(0L);
    }

    @Test
    public void countWithoutStreamFilter() {
        indexTestDocuments();

        final long count = adapter.count(Set.of(INDEX_NAME), Optional.empty());
        assertThat(count).isEqualTo(4L);
    }

    @Test
    public void countWithStreamFilter() {
        indexTestDocuments();

        final long countStream1 = adapter.count(Set.of(INDEX_NAME), Optional.of(Set.of(STREAM_1)));
        assertThat(countStream1).isEqualTo(3L);

        final long countStream2 = adapter.count(Set.of(INDEX_NAME), Optional.of(Set.of(STREAM_2)));
        assertThat(countStream2).isEqualTo(2L);

        final long countBothStreams = adapter.count(Set.of(INDEX_NAME), Optional.of(Set.of(STREAM_1, STREAM_2)));
        assertThat(countBothStreams).isEqualTo(4L);
    }

    @Test
    public void fieldHistogramReturnsGroupedResults() {
        indexTestDocuments();

        final Map<DateTime, Map<String, Long>> result = adapter.fieldHistogram(
                "source", Set.of(INDEX_NAME), Optional.empty(), 3600000L
        );

        assertThat(result).isNotEmpty();

        long totalSourceCounts = result.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToLong(Long::longValue)
                .sum();
        assertThat(totalSourceCounts).isEqualTo(4L);
    }

    @Test
    public void fieldHistogramWithStreamFilter() {
        indexTestDocuments();

        final Map<DateTime, Map<String, Long>> result = adapter.fieldHistogram(
                "source", Set.of(INDEX_NAME), Optional.of(Set.of(STREAM_1)), 3600000L
        );

        assertThat(result).isNotEmpty();

        long totalSourceCounts = result.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToLong(Long::longValue)
                .sum();
        assertThat(totalSourceCounts).isEqualTo(3L);
    }

    private void indexTestDocuments() {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZoneUTC();
        final String timestamp = formatter.print(DateTime.now(DateTimeZone.UTC).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0));

        final BulkIndexRequest bulkRequest = new BulkIndexRequest();

        bulkRequest.addRequest(INDEX_NAME, Map.of(
                "message", "test message 1",
                "source", "source-a",
                "timestamp", timestamp,
                "streams", java.util.List.of(STREAM_1)
        ));

        bulkRequest.addRequest(INDEX_NAME, Map.of(
                "message", "test message 2",
                "source", "source-a",
                "timestamp", timestamp,
                "streams", java.util.List.of(STREAM_1, STREAM_2)
        ));

        bulkRequest.addRequest(INDEX_NAME, Map.of(
                "message", "test message 3",
                "source", "source-b",
                "timestamp", timestamp,
                "streams", java.util.List.of(STREAM_2)
        ));

        bulkRequest.addRequest(INDEX_NAME, Map.of(
                "message", "test message 4",
                "source", "source-a",
                "timestamp", timestamp,
                "streams", java.util.List.of(STREAM_1)
        ));

        client().bulkIndex(bulkRequest);
        client().refreshNode();
    }
}
