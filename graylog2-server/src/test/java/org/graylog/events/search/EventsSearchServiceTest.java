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
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsSearchServiceTest {
    private static final DateTime NOW = DateTime.parse("2023-01-01T00:00:00Z");

    @Mock
    private MoreSearch moreSearch;
    @Mock
    private StreamService streamService;
    @Mock
    private DBEventDefinitionService eventDefinitionService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Subject subject;
    @Mock
    private ScriptingApiService scriptingApiService;

    private EventsSearchService service;

    @BeforeEach
    void setUp() {
        service = new EventsSearchService(moreSearch, streamService, eventDefinitionService, scriptingApiService, objectMapper);

        when(objectMapper.convertValue(any(Map.class), eq(EventDto.class))).thenAnswer(invocation -> {
            final Map<String, Object> fields = invocation.getArgument(0);
            final String eventDefId = (String) fields.get(EventDto.FIELD_EVENT_DEFINITION_ID);
            return eventDto(eventDefId);
        });
    }

    @Test
    void searchRespectsSubjectPermissions() {
        final EventsSearchParameters parameters = EventsSearchParameters.builder().query("message:*").build();
        final List<ResultMessage> results = List.of(
                resultMessage("event-def-allowed", Set.of("stream-allowed"), "index-a"),
                resultMessage("event-def-denied", Set.of("stream-denied"), "index-b"));
        final MoreSearch.Result searchResult = MoreSearch.Result.builder()
                .results(results)
                .resultsCount(results.size())
                .duration(1)
                .usedIndexNames(Set.of("index-a"))
                .executedQuery("message:*")
                .build();

        when(moreSearch.eventSearch(any(), any(), anySet(), anySet())).thenReturn(searchResult);
        when(streamService.streamAllIds()).thenAnswer(invocation -> java.util.stream.Stream.of("stream-allowed", "stream-denied"));

        mockSubjectPermissions();
        mockStreamLookups(Map.of("stream-allowed", stream("stream-allowed", "Allowed stream")));
        mockEventDefinitionLookups(Map.of(
                "event-def-allowed", eventDefinition("event-def-allowed")));

        final EventsSearchResult result = service.search(parameters, subject);

        assertThat(result.context().streams()).containsOnlyKeys("stream-allowed");
        assertThat(result.context().eventDefinitions()).containsOnlyKeys("event-def-allowed");

        final ArgumentCaptor<Set<String>> forbiddenCaptor = ArgumentCaptor.forClass(Set.class);
        final ArgumentCaptor<Set<String>> eventStreamsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(moreSearch).eventSearch(eq(parameters), any(), eventStreamsCaptor.capture(), forbiddenCaptor.capture());
        assertThat(eventStreamsCaptor.getValue()).containsExactly(DEFAULT_EVENTS_STREAM_ID);
        assertThat(forbiddenCaptor.getValue()).contains("stream-denied");
    }

    private void mockSubjectPermissions() {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(false);
        when(subject.isPermitted(permission(RestPermissions.STREAMS_READ, DEFAULT_EVENTS_STREAM_ID))).thenReturn(true);
        when(subject.isPermitted(permission(RestPermissions.STREAMS_READ, DEFAULT_SYSTEM_EVENTS_STREAM_ID))).thenReturn(false);
        when(subject.isPermitted(permission(RestPermissions.STREAMS_READ, "stream-allowed"))).thenReturn(true);
        when(subject.isPermitted(permission(RestPermissions.STREAMS_READ, "stream-denied"))).thenReturn(false);
        when(subject.isPermitted(permission(RestPermissions.EVENT_DEFINITIONS_READ, "event-def-allowed"))).thenReturn(true);
        when(subject.isPermitted(permission(RestPermissions.EVENT_DEFINITIONS_READ, "event-def-denied"))).thenReturn(false);
    }

    private static String permission(String base, String id) {
        return String.join(":", base, id);
    }

    private void mockStreamLookups(Map<String, Stream> streams) {
        when(streamService.loadByIds(any())).thenAnswer(invocation -> {
            final Collection<String> ids = invocation.getArgument(0);
            return ids.stream()
                    .map(streams::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        });
    }

    private void mockEventDefinitionLookups(Map<String, EventDefinitionDto> definitions) {
        when(eventDefinitionService.getByIds(any())).thenAnswer(invocation -> {
            final Collection<String> ids = invocation.getArgument(0);
            return ids.stream()
                    .map(definitions::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        });
    }

    private static Stream stream(String id, String title) {
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn(id);
        when(stream.getTitle()).thenReturn(title);
        when(stream.getDescription()).thenReturn("desc " + id);
        return stream;
    }

    private static EventDefinitionDto eventDefinition(String id) {
        final EventDefinitionDto dto = mock(EventDefinitionDto.class);
        when(dto.id()).thenReturn(id);
        when(dto.title()).thenReturn("title " + id);
        when(dto.description()).thenReturn("description " + id);
        return dto;
    }

    private static ResultMessage resultMessage(String eventDefinitionId, Set<String> streamIds, String index) {
        final Map<String, Object> fields = new HashMap<>();
        fields.put(EventDto.FIELD_EVENT_DEFINITION_ID, eventDefinitionId);
        final Message message = mock(Message.class);
        when(message.getFields()).thenReturn(fields);
        when(message.getField(EventDto.FIELD_EVENT_DEFINITION_ID)).thenReturn(eventDefinitionId);
        when(message.getStreamIds()).thenReturn(streamIds);

        final ResultMessage resultMessage = mock(ResultMessage.class);
        when(resultMessage.getMessage()).thenReturn(message);
        when(resultMessage.getIndex()).thenReturn(index);
        return resultMessage;
    }

    private static EventDto eventDto(String eventDefinitionId) {
        return EventDto.builder()
                .id(eventDefinitionId + "-event")
                .eventDefinitionType("type")
                .eventDefinitionId(eventDefinitionId)
                .originContext(null)
                .eventTimestamp(NOW)
                .processingTimestamp(NOW)
                .timerangeStart(NOW)
                .timerangeEnd(NOW)
                .streams(Set.of())
                .sourceStreams(Set.of())
                .message("message")
                .source("source")
                .keyTuple(List.of())
                .key(null)
                .priority(1)
                .scores(Map.of())
                .associatedAssets(Set.of())
                .alert(false)
                .fields(Map.of())
                .groupByFields(Map.of())
                .aggregationConditions(Map.of())
                .replayInfo(null)
                .build();
    }
}
