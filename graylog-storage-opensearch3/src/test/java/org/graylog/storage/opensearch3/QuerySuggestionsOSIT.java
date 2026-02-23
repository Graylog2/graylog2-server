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
package org.graylog.storage.opensearch3;

import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionEntry;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionFieldType;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionRequest;
import org.graylog.plugins.views.search.engine.suggestions.SuggestionResponse;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.storage.opensearch3.testing.OpenSearchTestServerExtension;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OpenSearchTestServerExtension.class)
class QuerySuggestionsOSIT {

    private static final String TEST_INDEX = "test_suggestions";
    private static final String STREAM_ID = "stream1";
    private static final String TIMESTAMP = "2025-06-15 10:00:00.000";
    private static final String GRAYLOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS||strict_date_optional_time||epoch_millis";

    private QuerySuggestionsOS querySuggestions;
    private OfficialOpensearchClient client;

    @BeforeEach
    void setUp(OpenSearchInstance openSearchInstance) {
        client = openSearchInstance.getOfficialOpensearchClient();

        client.sync(c -> c.indices().create(r -> r
                .index(TEST_INDEX)
                .mappings(m -> m
                        .properties("source", p -> p.keyword(k -> k))
                        .properties("message", p -> p.text(t -> t))
                        .properties("streams", p -> p.keyword(k -> k))
                        .properties("timestamp", p -> p.date(d -> d.format(GRAYLOG_DATE_FORMAT)))
                )
        ), "Failed to create test index");

        indexDocument(Map.of("source", "apache", "message", "request handled", "streams", Set.of(STREAM_ID), "timestamp", TIMESTAMP));
        indexDocument(Map.of("source", "apache2", "message", "request timeout", "streams", Set.of(STREAM_ID), "timestamp", TIMESTAMP));
        indexDocument(Map.of("source", "apache3", "message", "request error", "streams", Set.of(STREAM_ID), "timestamp", TIMESTAMP));
        indexDocument(Map.of("source", "nginx", "message", "connection reset", "streams", Set.of(STREAM_ID), "timestamp", TIMESTAMP));
        indexDocument(Map.of("source", "nginx2", "message", "connection refused", "streams", Set.of(STREAM_ID), "timestamp", TIMESTAMP));

        client.sync(c -> c.indices().refresh(r -> r.index(TEST_INDEX)), "Failed to refresh index");

        final IndexLookup indexLookup = mock(IndexLookup.class);
        when(indexLookup.indexNamesForStreamsInTimeRange(anyCollection(), any())).thenReturn(Set.of(TEST_INDEX));
        querySuggestions = new QuerySuggestionsOS(client, indexLookup);
    }

    @AfterEach
    void tearDown() {
        client.sync(c -> c.indices().delete(r -> r.index(TEST_INDEX).ignoreUnavailable(true)),
                "Failed to delete test index");
    }

    private AbsoluteRange testTimeRange() {
        return AbsoluteRange.create(
                DateTime.parse("2025-01-01T00:00:00.000Z").withZone(DateTimeZone.UTC),
                DateTime.parse("2025-12-31T23:59:59.999Z").withZone(DateTimeZone.UTC));
    }

    @Test
    void shouldReturnAggregationResultsForMatchingPrefix() {
        final SuggestionRequest request = SuggestionRequest.builder()
                .field("source")
                .input("apa")
                .fieldType(SuggestionFieldType.OTHER)
                .streams(Set.of(STREAM_ID))
                .timerange(testTimeRange())
                .size(10)
                .build();

        final SuggestionResponse response = querySuggestions.suggest(request, Duration.ofSeconds(10));

        assertThat(response.suggestionError()).isEmpty();
        assertThat(response.suggestions()).hasSize(3);
        assertThat(response.suggestions().stream().map(SuggestionEntry::getValue))
                .containsExactlyInAnyOrder("apache", "apache2", "apache3");
        assertThat(response.field()).isEqualTo("source");
        assertThat(response.input()).isEqualTo("apa");
    }

    @Test
    void shouldReturnEmptySuggestionsWhenNoPrefixMatches() {
        final SuggestionRequest request = SuggestionRequest.builder()
                .field("source")
                .input("xyz")
                .fieldType(SuggestionFieldType.OTHER)
                .streams(Set.of(STREAM_ID))
                .timerange(testTimeRange())
                .size(10)
                .build();

        final SuggestionResponse response = querySuggestions.suggest(request, Duration.ofSeconds(10));

        assertThat(response.suggestionError()).isEmpty();
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    void shouldReturnSingleMatchForExactPrefix() {
        final SuggestionRequest request = SuggestionRequest.builder()
                .field("source")
                .input("nginx2")
                .fieldType(SuggestionFieldType.OTHER)
                .streams(Set.of(STREAM_ID))
                .timerange(testTimeRange())
                .size(10)
                .build();

        final SuggestionResponse response = querySuggestions.suggest(request, Duration.ofSeconds(10));

        assertThat(response.suggestionError()).isEmpty();
        assertThat(response.suggestions()).hasSize(1);
        assertThat(response.suggestions().get(0).getValue()).isEqualTo("nginx2");
    }

    @Test
    void shouldRespectSizeParameter() {
        final SuggestionRequest request = SuggestionRequest.builder()
                .field("source")
                .input("apa")
                .fieldType(SuggestionFieldType.OTHER)
                .streams(Set.of(STREAM_ID))
                .timerange(testTimeRange())
                .size(2)
                .build();

        final SuggestionResponse response = querySuggestions.suggest(request, Duration.ofSeconds(10));

        assertThat(response.suggestionError()).isEmpty();
        assertThat(response.suggestions()).hasSize(2);
        assertThat(response.sumOtherDocsCount()).isEqualTo(1L);
    }

    @Test
    void shouldReturnEmptyResultsForNonExistentIndex() {
        final IndexLookup badLookup = mock(IndexLookup.class);
        when(badLookup.indexNamesForStreamsInTimeRange(anyCollection(), any())).thenReturn(Set.of("nonexistent_index"));
        final QuerySuggestionsOS badSuggestions = new QuerySuggestionsOS(client, badLookup);

        final SuggestionRequest request = SuggestionRequest.builder()
                .field("source")
                .input("apa")
                .fieldType(SuggestionFieldType.OTHER)
                .streams(Set.of(STREAM_ID))
                .timerange(testTimeRange())
                .size(10)
                .build();

        // With ignoreUnavailable=true, non-existent indices are silently ignored
        final SuggestionResponse response = badSuggestions.suggest(request, Duration.ofSeconds(10));

        assertThat(response.suggestionError()).isEmpty();
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    void shouldWorkWithTextualFieldType() {
        final SuggestionRequest request = SuggestionRequest.builder()
                .field("source")
                .input("apa")
                .fieldType(SuggestionFieldType.TEXTUAL)
                .streams(Set.of(STREAM_ID))
                .timerange(testTimeRange())
                .size(10)
                .build();

        final SuggestionResponse response = querySuggestions.suggest(request, Duration.ofSeconds(10));

        assertThat(response.suggestionError()).isEmpty();
        assertThat(response.suggestions()).hasSize(3);
        assertThat(response.suggestions().stream().map(SuggestionEntry::getValue))
                .containsExactlyInAnyOrder("apache", "apache2", "apache3");
    }

    private void indexDocument(Map<String, Object> doc) {
        client.sync(c -> c.index(r -> r
                .index(TEST_INDEX)
                .document(doc)
        ), "Failed to index document");
    }
}
