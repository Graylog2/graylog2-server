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
package org.graylog.inputs.otel.transport;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentCertAuthInterceptorTest {

    private AgentCertAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AgentCertAuthInterceptor();
    }

    @SuppressWarnings("unchecked")
    @Test
    void interceptCallSetsContextKeyWhenUidPresent() {
        final ServerCall<Object, Object> call = mock(ServerCall.class);
        final Attributes attrs = Attributes.newBuilder()
                .set(AgentCertTransportFilter.AGENT_INSTANCE_UID_KEY, "agent-123")
                .build();
        when(call.getAttributes()).thenReturn(attrs);
        when(call.getMethodDescriptor()).thenReturn(mock(MethodDescriptor.class));

        final ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
        final ServerCall.Listener<Object> expectedListener = new ServerCall.Listener<>() {};
        when(next.startCall(any(), any())).thenReturn(expectedListener);

        final ServerCall.Listener<Object> result =
                interceptor.interceptCall(call, new Metadata(), next);

        // Verify next handler was called (not closed with error)
        verify(next).startCall(any(), any());
        verify(call, never()).close(any(), any());
        // Contexts.interceptCall wraps the listener, so we just verify it's not null
        assertThat(result).isNotNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void interceptCallRejectsWhenNoUid() {
        final ServerCall<Object, Object> call = mock(ServerCall.class);
        final Attributes attrs = Attributes.newBuilder().build();
        when(call.getAttributes()).thenReturn(attrs);
        when(call.getMethodDescriptor()).thenReturn(mock(MethodDescriptor.class));

        final ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);

        interceptor.interceptCall(call, new Metadata(), next);

        final ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(statusCaptor.getValue().getDescription()).isEqualTo("No agent certificate");

        // Verify next handler was NOT called
        verify(next, never()).startCall(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void interceptCallMakesUidAvailableInContext() {
        final ServerCall<Object, Object> call = mock(ServerCall.class);
        final Attributes attrs = Attributes.newBuilder()
                .set(AgentCertTransportFilter.AGENT_INSTANCE_UID_KEY, "agent-456")
                .build();
        when(call.getAttributes()).thenReturn(attrs);
        when(call.getMethodDescriptor()).thenReturn(mock(MethodDescriptor.class));

        // Capture the context value when next.startCall is invoked
        final String[] capturedUid = new String[1];
        final ServerCallHandler<Object, Object> next = (c, headers) -> {
            capturedUid[0] = AgentCertAuthInterceptor.AGENT_INSTANCE_UID.get();
            return new ServerCall.Listener<>() {};
        };

        interceptor.interceptCall(call, new Metadata(), next);

        assertThat(capturedUid[0]).isEqualTo("agent-456");
    }
}
