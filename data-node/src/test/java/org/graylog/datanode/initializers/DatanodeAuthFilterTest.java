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
package org.graylog.datanode.initializers;

import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.glassfish.jersey.server.ContainerRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.mock;

class DatanodeAuthFilterTest {

    @Test
    void testValidToken() throws IOException {
        final ContainerRequestFilter fallbackFilter = Mockito.mock(ContainerRequestFilter.class);

        final DatanodeAuthFilter datanodeAuthFilter = new DatanodeAuthFilter(fallbackFilter, tokenValidator("123456789"));

        final ContainerRequest request = mockRequest("Bearer 123456789");

        datanodeAuthFilter.filter(request);

        Mockito.verify(fallbackFilter, Mockito.never()).filter(Mockito.any());
        Mockito.verify(request, Mockito.never()).abortWith(Mockito.any());
    }

    @NotNull
    private static AuthTokenValidator tokenValidator(String expectedTokenValue) {
        return token -> {
            if (!expectedTokenValue.equals(token)) {
                throw new TokenVerificationException("Invalid token!");
            }
        };
    }

    @Test
    void testFallbackFilter() throws IOException {
        final ContainerRequestFilter fallbackFilter = Mockito.mock(ContainerRequestFilter.class);
        final DatanodeAuthFilter datanodeAuthFilter = new DatanodeAuthFilter(fallbackFilter, tokenValidator("123456789"));

        // do not provide any header in the request(=null), we want to see the fallback kick in
        final ContainerRequest request = mockRequest(null);

        datanodeAuthFilter.filter(request);

        Mockito.verify(fallbackFilter, Mockito.times(1)).filter(Mockito.any());
        Mockito.verify(request, Mockito.never()).abortWith(Mockito.any());
    }

    @Test
    void testInvalidToken() throws IOException {

        final ContainerRequestFilter fallbackFilter = Mockito.mock(ContainerRequestFilter.class);
        final DatanodeAuthFilter datanodeAuthFilter = new DatanodeAuthFilter(fallbackFilter, tokenValidator("123456789"));

        // this is not a token we see as valid. The filter should recognize that and abort the request
        final ContainerRequest request = mockRequest("Bearer AABBCCDDEE");

        datanodeAuthFilter.filter(request);

        Mockito.verify(fallbackFilter, Mockito.never()).filter(Mockito.any());
        Mockito.verify(request, Mockito.times(1)).abortWith(Mockito.any());
    }

    private static ContainerRequest mockRequest(String bearerToken) {
        final ContainerRequest request = mock(ContainerRequest.class);
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        Optional.ofNullable(bearerToken).ifPresent(token -> headers.put("Authorization", Collections.singletonList(token)));
        Mockito.when(request.getHeaders()).thenReturn(headers);
        return request;
    }
}
