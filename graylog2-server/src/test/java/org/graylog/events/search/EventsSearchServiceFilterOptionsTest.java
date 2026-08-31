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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.subject.Subject;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.resources.entities.Slice;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsSearchServiceFilterOptionsTest {
    private static final String SOURCE_STREAM = "source-stream-allowed";
    private static final String OTHER_STREAM = "source-stream-denied";

    @Mock
    private MoreSearch moreSearch;
    @Mock
    private StreamService streamService;
    @Mock
    private DBEventDefinitionService eventDefinitionService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EventDefinitionFilterFactory eventDefinitionFilterFactory;
    @Mock
    private Subject subject;

    private EventsSearchService service;

    @BeforeEach
    void setUp() {
        service = new EventsSearchService(moreSearch, streamService, eventDefinitionService, objectMapper,
                eventDefinitionFilterFactory);
    }

    @Test
    void returnsTagsInAggregationOrderForSubjectWithoutGlobalStreamPermission() {
        mockSourceStreamOnlyPermissions();
        mockAggregation(List.of(slice("windows"), slice("credential-access"), slice("linux")));

        final var result = service.filterOptions(request(List.of(EventDto.FIELD_TAGS), ""), subject);

        // The aggregation returns the most used values first; that order must be preserved.
        assertThat(result.tags()).containsExactly("windows", "credential-access", "linux");
    }

    @Test
    void scopesAggregationToTheSourceStreamsTheSubjectMayRead() {
        mockSourceStreamOnlyPermissions();
        mockAggregation(List.of(slice("windows")));

        service.filterOptions(request(List.of(EventDto.FIELD_TAGS), ""), subject);

        final ArgumentCaptor<Set<String>> eventStreams = ArgumentCaptor.forClass(Set.class);
        final ArgumentCaptor<SourceStreamFilter> sourceStreams = ArgumentCaptor.forClass(SourceStreamFilter.class);
        verify(moreSearch).aggregateSlicesForColumn(anyString(), any(), eventStreams.capture(), anyString(),
                sourceStreams.capture(), any(), eq(EventDto.FIELD_TAGS), any(), anyMap(), eq(50));

        assertThat(eventStreams.getValue()).containsExactly(DEFAULT_EVENTS_STREAM_ID);
        assertThat(sourceStreams.getValue().isAllAllowed()).isFalse();
        assertThat(sourceStreams.getValue().streamIds()).containsExactly(SOURCE_STREAM);
    }

    @Test
    void passesTheRequestQueryToTheAggregation() {
        mockSourceStreamOnlyPermissions();
        mockAggregation(List.of(slice("windows")));

        service.filterOptions(request(List.of(EventDto.FIELD_TAGS), "source_streams:" + SOURCE_STREAM), subject);

        verify(moreSearch).aggregateSlicesForColumn(eq("source_streams:" + SOURCE_STREAM), any(), anySet(),
                anyString(), any(), any(), eq(EventDto.FIELD_TAGS), isNull(), anyMap(), anyInt());
    }

    @Test
    void buildsAContainsPatternFromTheFieldQuery() {
        mockSourceStreamOnlyPermissions();
        mockAggregation(List.of(slice("credential-access")));

        service.filterOptions(request(List.of(EventDto.FIELD_TAGS), "", "Access"), subject);

        verify(moreSearch).aggregateSlicesForColumn(anyString(), any(), anySet(), anyString(), any(), any(),
                eq(EventDto.FIELD_TAGS), eq(".*access.*"), anyMap(), anyInt());
    }

    @Test
    void escapesSupplementaryCharactersInTheFieldQueryAsWholeCodePoints() {
        mockSourceStreamOnlyPermissions();
        mockAggregation(List.of());

        service.filterOptions(request(List.of(EventDto.FIELD_TAGS), "", "a🦊b"), subject);

        // The surrogate pair must stay together behind a single escape, not become two escaped halves.
        verify(moreSearch).aggregateSlicesForColumn(anyString(), any(), anySet(), anyString(), any(), any(),
                eq(EventDto.FIELD_TAGS), eq(".*a\\🦊b.*"), anyMap(), anyInt());
    }

    @Test
    void capsTheFieldQueryLength() {
        final var request = new EventsFilterOptionsRequest(List.of(), "", "a".repeat(1000), null);

        assertThat(request.fieldQuery()).hasSize(256);
    }

    @Test
    void appliesRequestDefaults() throws Exception {
        final var request = new EventsFilterOptionsRequest(null, null, null, null);

        assertThat(request.fields()).isEmpty();
        assertThat(request.query()).isEmpty();
        assertThat(request.fieldQuery()).isEmpty();
        // The default time range for collecting filter values is the last 30 days.
        assertThat(request.timerange()).isEqualTo(RelativeRange.create(30 * 24 * 60 * 60));
    }

    @Test
    void escapesRegexMetacharactersInTheFieldQuery() {
        mockSourceStreamOnlyPermissions();
        mockAggregation(List.of(slice("t1003.001")));

        service.filterOptions(request(List.of(EventDto.FIELD_TAGS), "", "T1003.001"), subject);

        verify(moreSearch).aggregateSlicesForColumn(anyString(), any(), anySet(), anyString(), any(), any(),
                eq(EventDto.FIELD_TAGS), eq(".*t1003\\.001.*"), anyMap(), anyInt());
    }

    @Test
    void skipsValuesWithoutATag() {
        mockSourceStreamOnlyPermissions();
        mockAggregation(List.of(slice("windows"), slice(null), slice("")));

        final var result = service.filterOptions(request(List.of(EventDto.FIELD_TAGS), ""), subject);

        assertThat(result.tags()).containsExactly("windows");
    }

    @Test
    void omitsFieldsThatWereNotRequested() {
        final var result = service.filterOptions(request(List.of(), ""), subject);

        assertThat(result.tags()).isNull();
        verifyNoInteractions(moreSearch);
    }

    private void mockSourceStreamOnlyPermissions() {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(false);
        when(subject.isPermitted(permission(DEFAULT_EVENTS_STREAM_ID))).thenReturn(false);
        when(subject.isPermitted(permission(DEFAULT_SYSTEM_EVENTS_STREAM_ID))).thenReturn(false);
        when(subject.isPermitted(permission(SOURCE_STREAM))).thenReturn(true);
        when(subject.isPermitted(permission(OTHER_STREAM))).thenReturn(false);
        when(streamService.streamAllIds()).thenAnswer(invocation -> java.util.stream.Stream.of(SOURCE_STREAM, OTHER_STREAM));
    }

    private void mockAggregation(List<Slice> slices) {
        when(moreSearch.aggregateSlicesForColumn(anyString(), any(), anySet(), anyString(), any(), any(),
                anyString(), any(), anyMap(), anyInt())).thenReturn(slices);
    }

    private static EventsFilterOptionsRequest request(List<String> fields, String query) {
        return request(fields, query, "");
    }

    private static EventsFilterOptionsRequest request(List<String> fields, String query, String fieldQuery) {
        return new EventsFilterOptionsRequest(fields, query, fieldQuery, RelativeRange.allTime());
    }

    private static String permission(String streamId) {
        return String.join(":", RestPermissions.STREAMS_READ, streamId);
    }

    private static Slice slice(String value) {
        return new Slice(value, null, 1, Map.of());
    }
}
