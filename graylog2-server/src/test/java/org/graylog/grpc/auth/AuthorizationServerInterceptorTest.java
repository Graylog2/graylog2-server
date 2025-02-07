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
package org.graylog.grpc.auth;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthorizationServerInterceptorTest<S, T> {
    public static final Metadata.Key<String> METADATA_KEY = Metadata.Key.of("api-token", Metadata.ASCII_STRING_MARSHALLER);

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ServerCall<S, T> call;

    @Mock
    private ServerCallHandler<S, T> next;

    @Mock
    private ShiroAuthenticator shiroAuthenticator;

    private AuthorizationServerInterceptor interceptor;

    final Attributes callAttributes = Attributes.newBuilder()
            .set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, new InetSocketAddress("127.0.0.1", 13301))
            .build();

    @Before
    public void setUp() throws Exception {
        interceptor = new AuthorizationServerInterceptor(shiroAuthenticator, Collections.emptySet(),
                headers -> Optional.ofNullable(headers.get(METADATA_KEY)));
    }

    @Test
    public void closesCallIfNoAccessToken() {
        final ServerCall.Listener<?> listener = interceptor.interceptCall(call, new Metadata(), next);

        assertThat(listener).isNotNull();
        verify(call).close(argThat(status -> status.getCode()
                .equals(Status.Code.UNAUTHENTICATED)), any(Metadata.class));
    }

    @Test
    public void closesCallIfAuthenticationFails() {
        when(call.getAttributes()).thenReturn(callAttributes);
        doThrow(new AuthenticationException("Unauthenticated")).when(shiroAuthenticator)
                .authenticate(anyString(), eq("invalid-token"));

        final Metadata headers = new Metadata();
        headers.put(METADATA_KEY, "invalid-token");
        final ServerCall.Listener<?> listener = interceptor.interceptCall(call, headers, next);

        assertThat(listener).isNotNull();
        verify(call).close(argThat(status -> status.getCode()
                .equals(Status.Code.UNAUTHENTICATED)), any(Metadata.class));
    }

    @Test
    public void closesCallIfInternalError() {
        when(call.getAttributes()).thenReturn(callAttributes);
        doThrow(new RuntimeException("internal error")).when(shiroAuthenticator)
                .authenticate(anyString(), eq("existing-token"));

        final Metadata headers = new Metadata();
        headers.put(METADATA_KEY, "existing-token");
        final ServerCall.Listener<?> listener = interceptor.interceptCall(call, headers, next);

        assertThat(listener).isNotNull();
        verify(call).close(argThat(status -> status.getCode()
                .equals(Status.Code.INTERNAL)), any(Metadata.class));
    }

    @Test
    public void letsCallPassIfAuthenticated() {
        when(call.getAttributes()).thenReturn(callAttributes);
        when(shiroAuthenticator.authenticate(anyString(), eq("existing-token"))).thenReturn(mock(Subject.class));

        final Metadata headers = new Metadata();
        headers.put(METADATA_KEY, "existing-token");
        final ServerCall.Listener<?> listener = interceptor.interceptCall(call, headers, next);

        assertThat(listener).isNotNull();
        verify(call, never()).close(any(), any());
    }

    @Test
    public void passesTokenExtractionException() {
        final var interceptorWithThrowingTokenExtractor = new AuthorizationServerInterceptor(
                shiroAuthenticator,
                Collections.emptySet(),
                t -> {throw new IllegalArgumentException("Did not expect a teapot!");});
        final Metadata headers = new Metadata();
        headers.put(METADATA_KEY, "i-am-a-teapot");
        final ServerCall.Listener<?> listener = interceptorWithThrowingTokenExtractor.interceptCall(call, headers, next);

        assertThat(listener).isNotNull();
        verify(call).close(argThat(status ->
                status.getCode().equals(Status.Code.UNAUTHENTICATED) &&
                        status.getDescription() != null &&
                        status.getDescription().contains("Did not expect a teapot!")), any(Metadata.class));
    }
}
