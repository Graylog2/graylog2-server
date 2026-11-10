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
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.indexer.searches.timeranges.RelativeRange.allTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsSliceServiceTest {

    @Mock
    private ScriptingApiService scriptingApiService;
    @Mock
    private StreamService streamService;
    @Mock
    private DBEventDefinitionService eventDefinitionService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Subject subject;
    @Mock
    private SearchUser searchUser;

    private EventsSliceService service;

    @BeforeEach
    void setUp() {
        service = new EventsSliceService(scriptingApiService, streamService, eventDefinitionService, objectMapper);
    }

    @Test
    void slicesFiltersQueryByAllowedSourceStreams() throws Exception {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(false);
        when(subject.isPermitted("streams:read:stream-a")).thenReturn(true);
        when(subject.isPermitted("streams:read:stream-b")).thenReturn(false);
        when(subject.isPermitted("streams:read:000000000000000000000002")).thenReturn(true);
        when(subject.isPermitted("streams:read:000000000000000000000003")).thenReturn(false);
        when(streamService.streamAllIds()).thenReturn(Stream.of("stream-a", "stream-b"));
        when(scriptingApiService.executeAggregation(any(), any())).thenReturn(new TabularResponse(List.of(), List.of(), null));

        service.slices("message:test", allTime(), subject, searchUser, "priority", Map.of());

        final var captor = ArgumentCaptor.forClass(AggregationRequestSpec.class);
        verify(scriptingApiService).executeAggregation(captor.capture(), any());
        assertThat(captor.getValue().queryString()).contains("source_streams:(stream-a)");
        assertThat(captor.getValue().queryString()).contains("message:test");
    }

    @Test
    void slicesReturnsEmptyWhenNoSourceStreamPermissions() {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(false);
        when(subject.isPermitted("streams:read:stream-a")).thenReturn(false);
        when(streamService.streamAllIds()).thenReturn(Stream.of("stream-a"));

        final var result = service.slices("", allTime(), subject, searchUser, "priority", Map.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(scriptingApiService);
    }

    @Test
    void slicesPassesQueryUnmodifiedWhenAllStreamsAllowed() throws Exception {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(true);
        when(scriptingApiService.executeAggregation(any(), any())).thenReturn(new TabularResponse(List.of(), List.of(), null));

        service.slices("message:test", allTime(), subject, searchUser, "priority", Map.of());

        final var captor = ArgumentCaptor.forClass(AggregationRequestSpec.class);
        verify(scriptingApiService).executeAggregation(captor.capture(), any());
        assertThat(captor.getValue().queryString()).isEqualTo("message:test");
    }

    @Test
    void countReturnsEmptyWhenNoSourceStreamPermissions() {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(false);
        when(subject.isPermitted("streams:read:stream-a")).thenReturn(false);
        when(streamService.streamAllIds()).thenReturn(Stream.of("stream-a"));

        final var result = service.count("", allTime(), subject, searchUser, Map.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(scriptingApiService);
    }
}
