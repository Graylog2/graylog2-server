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
package org.graylog2.shared.security;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.glassfish.grizzly.http.server.Request;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShiroSecurityContextFilterTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private SecurityContext securityContext;

    private ShiroSecurityContextFilter filter;

    @BeforeClass
    public static void setUpInjector() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Before
    public void setUp() throws Exception {
        when(securityContext.isSecure()).thenReturn(false);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);

        final DefaultSecurityManager securityManager = new DefaultSecurityManager();
        final Provider<Request> grizzlyRequestProvider = () -> mock(Request.class);
        filter = new ShiroSecurityContextFilter(securityManager, grizzlyRequestProvider, Collections.emptySet());
    }

    @Test
    public void filterWithoutAuthorizationHeaderShouldDoNothing() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        final ArgumentCaptor<SecurityContext> argument = ArgumentCaptor.forClass(SecurityContext.class);
        verify(requestContext).setSecurityContext(argument.capture());
        assertThat(argument.getValue()).isExactlyInstanceOf(ShiroSecurityContext.class);
        assertThat(argument.getValue().getAuthenticationScheme()).isNull();
    }

    @Test
    public void filterWithNonBasicAuthorizationHeaderShouldDoNothing() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(HttpHeaders.AUTHORIZATION, "Foobar");
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        final ArgumentCaptor<SecurityContext> argument = ArgumentCaptor.forClass(SecurityContext.class);
        verify(requestContext).setSecurityContext(argument.capture());
        assertThat(argument.getValue()).isExactlyInstanceOf(ShiroSecurityContext.class);
        assertThat(argument.getValue().getAuthenticationScheme()).isNull();
    }

    @Test(expected = BadRequestException.class)
    public void filterWithMalformedBasicAuthShouldThrowBadRequestException() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(HttpHeaders.AUTHORIZATION, "Basic ****");
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);
    }

    @Test(expected = BadRequestException.class)
    public void filterWithBasicAuthAndMalformedCredentialsShouldThrowBadRequestException() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        final String credentials = Base64.getEncoder().encodeToString("user_pass".getBytes(StandardCharsets.US_ASCII));
        headers.putSingle(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);
    }

    @Test
    public void filterWithBasicAuthAndCredentialsShouldCreateShiroSecurityContextWithUsernamePasswordToken() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        final String credentials = Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.US_ASCII));
        headers.putSingle(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        final ArgumentCaptor<ShiroSecurityContext> argument = ArgumentCaptor.forClass(ShiroSecurityContext.class);
        verify(requestContext).setSecurityContext(argument.capture());
        final ShiroSecurityContext securityContext = argument.getValue();
        assertThat(securityContext).isExactlyInstanceOf(ShiroSecurityContext.class);
        assertThat(securityContext.getAuthenticationScheme()).isEqualTo(SecurityContext.BASIC_AUTH);
        assertThat(securityContext.getUsername()).isEqualTo("user");
        assertThat(securityContext.getToken()).isExactlyInstanceOf(UsernamePasswordToken.class);
    }

    @Test
    public void filterWithBasicAuthAndSessionIdShouldCreateShiroSecurityContextWithSessionIdToken() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        final String credentials = Base64.getEncoder().encodeToString("test:session".getBytes(StandardCharsets.US_ASCII));
        headers.putSingle(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        final ArgumentCaptor<ShiroSecurityContext> argument = ArgumentCaptor.forClass(ShiroSecurityContext.class);
        verify(requestContext).setSecurityContext(argument.capture());
        final ShiroSecurityContext securityContext = argument.getValue();
        assertThat(securityContext).isExactlyInstanceOf(ShiroSecurityContext.class);
        assertThat(securityContext.getAuthenticationScheme()).isEqualTo(SecurityContext.BASIC_AUTH);
        assertThat(securityContext.getToken()).isExactlyInstanceOf(SessionIdToken.class);
    }

    @Test
    public void filterWithBasicAuthAndTokenShouldCreateShiroSecurityContextWithAccessTokenAuthToken() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        final String credentials = Base64.getEncoder().encodeToString("test:token".getBytes(StandardCharsets.US_ASCII));
        headers.putSingle(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        final ArgumentCaptor<ShiroSecurityContext> argument = ArgumentCaptor.forClass(ShiroSecurityContext.class);
        verify(requestContext).setSecurityContext(argument.capture());
        final ShiroSecurityContext securityContext = argument.getValue();
        assertThat(securityContext).isExactlyInstanceOf(ShiroSecurityContext.class);
        assertThat(securityContext.getAuthenticationScheme()).isEqualTo(SecurityContext.BASIC_AUTH);
        assertThat(securityContext.getToken()).isExactlyInstanceOf(AccessTokenAuthToken.class);
    }


    @Test
    public void filterWithBasicAuthAndPasswordWithColonShouldCreateShiroSecurityContextWithUsernamePasswordToken() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        final String credentials = Base64.getEncoder().encodeToString("user:pass:word".getBytes(StandardCharsets.US_ASCII));
        headers.putSingle(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        final ArgumentCaptor<ShiroSecurityContext> argument = ArgumentCaptor.forClass(ShiroSecurityContext.class);
        verify(requestContext).setSecurityContext(argument.capture());
        final ShiroSecurityContext securityContext = argument.getValue();
        assertThat(securityContext).isExactlyInstanceOf(ShiroSecurityContext.class);
        assertThat(securityContext.getAuthenticationScheme()).isEqualTo(SecurityContext.BASIC_AUTH);
        assertThat(securityContext.getUsername()).isEqualTo("user");
        assertThat(securityContext.getPassword()).isEqualTo("pass:word");
        assertThat(securityContext.getToken()).isExactlyInstanceOf(UsernamePasswordToken.class);
    }
}
