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
package org.graylog2.rest.resources.streams;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.subject.Subject;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamResourceTest {
    private static final String INDEX_SET_ID = "index-set-id";

    @Mock
    private StreamService streamService;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    @Mock
    private AuditEventSender auditEventSender;
    @Mock
    private UserContext userContext;

    private final AuditActor actor = AuditActor.user("admin");
    private final Stream stream1 = mockStream("stream-1", "Stream 1");
    private final Stream stream2 = mockStream("stream-2", "Stream 2");
    private final Subject subject = mock(Subject.class);
    private StreamResource resource;

    @BeforeEach
    void setUp() {
        when(subject.isPermitted(anyString())).thenReturn(true);
        resource = new StreamResource(streamService, null, null, null, null, indexSetRegistry, null,
                auditEventSender, null, null, null, null) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };

        final User user = mock(User.class);
        when(user.getName()).thenReturn("admin");
        when(userContext.getUser()).thenReturn(user);

        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(INDEX_SET_ID);
        when(config.title()).thenReturn("Index Set");
        when(config.isWritable()).thenReturn(true);
        when(config.isRegularIndex()).thenReturn(true);
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getConfig()).thenReturn(config);
        when(indexSetRegistry.get(INDEX_SET_ID)).thenReturn(Optional.of(indexSet));

        when(streamService.isEditable(anyString())).thenReturn(true);
        when(streamService.streamDTOByIds(anyList())).thenAnswer(invocation -> java.util.stream.Stream.of(stream1, stream2));
    }

    @Test
    void auditsEachAssignedStream() {
        resource.assignToIndexSet(INDEX_SET_ID, List.of("stream-1", "stream-2"), userContext);

        verify(auditEventSender).success(actor, AuditEventTypes.STREAM_ASSIGN_INDEX_SET, context("stream-1", "Stream 1"));
        verify(auditEventSender).success(actor, AuditEventTypes.STREAM_ASSIGN_INDEX_SET, context("stream-2", "Stream 2"));
    }

    @Test
    void auditsFailureForEachStreamWhenAssignmentFails() {
        final RuntimeException error = new RuntimeException("boom");
        doThrow(error).when(streamService).addToIndexSet(INDEX_SET_ID, List.of("stream-1", "stream-2"));

        assertThatThrownBy(() -> resource.assignToIndexSet(INDEX_SET_ID, List.of("stream-1", "stream-2"), userContext))
                .isSameAs(error);

        verify(auditEventSender).failure(actor, AuditEventTypes.STREAM_ASSIGN_INDEX_SET, context("stream-1", "Stream 1"));
        verify(auditEventSender).failure(actor, AuditEventTypes.STREAM_ASSIGN_INDEX_SET, context("stream-2", "Stream 2"));
    }

    @Test
    void auditsFailureForEachRequestedStreamWhenRejected() {
        when(subject.isPermitted("streams:edit:stream-2")).thenReturn(false);

        assertThatThrownBy(() -> resource.assignToIndexSet(INDEX_SET_ID, List.of("stream-1", "stream-2"), userContext))
                .isInstanceOf(ForbiddenException.class);

        verify(auditEventSender).failure(actor, AuditEventTypes.STREAM_ASSIGN_INDEX_SET, rejectedContext("stream-1"));
        verify(auditEventSender).failure(actor, AuditEventTypes.STREAM_ASSIGN_INDEX_SET, rejectedContext("stream-2"));
        verify(auditEventSender, never()).success(any(AuditActor.class), anyString(), anyMap());
    }

    @Test
    void auditsFailureForEachRequestedStreamWhenAStreamIsMissing() {
        final Response response = resource.assignToIndexSet(INDEX_SET_ID, List.of("stream-1", "stream-2", "stream-3"), userContext);

        assertThat(response.getStatus()).isEqualTo(404);
        verify(auditEventSender).failure(actor, AuditEventTypes.STREAM_ASSIGN_INDEX_SET, rejectedContext("stream-3"));
        verify(auditEventSender, never()).success(any(AuditActor.class), anyString(), anyMap());
    }

    private static Map<String, Object> rejectedContext(String streamId) {
        return Map.of("response_entity", Map.of("stream_id", streamId, "index_set_id", INDEX_SET_ID));
    }

    private static Stream mockStream(String id, String title) {
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn(id);
        when(stream.getTitle()).thenReturn(title);
        return stream;
    }

    private static Map<String, Object> context(String streamId, String streamTitle) {
        return Map.of("response_entity", Map.of(
                "stream_id", streamId,
                "title", streamTitle,
                "index_set_id", INDEX_SET_ID,
                "index_set_title", "Index Set"));
    }
}
