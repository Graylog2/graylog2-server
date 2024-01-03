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

import com.github.joschi.jadconfig.util.Duration;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang.RandomStringUtils;
import org.glassfish.jersey.server.ContainerRequest;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;

class DatanodeAuthFilterTest {

    @Test
    void testValidJwtToken() throws IOException {

        final String signingKey = RandomStringUtils.random(64);

        final IndexerJwtAuthTokenProvider tokenProvider = new IndexerJwtAuthTokenProvider(signingKey, Duration.seconds(10), Duration.seconds(1));
        final String token = tokenProvider.get();

        final ContainerRequestFilter fallbackFilter = Mockito.mock(ContainerRequestFilter.class);
        final DatanodeAuthFilter datanodeAuthFilter = new DatanodeAuthFilter(fallbackFilter, signingKey);

        final ContainerRequest request = mockRequest(token);

        datanodeAuthFilter.filter(request);

        Mockito.verify(fallbackFilter, Mockito.never()).filter(Mockito.any());
        Mockito.verify(request, Mockito.never()).abortWith(Mockito.any());
    }

    @Test
    void testFallbackFilter() throws IOException {

        final String signingKey = RandomStringUtils.random(64);
        final ContainerRequestFilter fallbackFilter = Mockito.mock(ContainerRequestFilter.class);
        final DatanodeAuthFilter datanodeAuthFilter = new DatanodeAuthFilter(fallbackFilter, signingKey);

        final ContainerRequest request = mockRequest(null);

        datanodeAuthFilter.filter(request);

        Mockito.verify(fallbackFilter, Mockito.times(1)).filter(Mockito.any());
        Mockito.verify(request, Mockito.never()).abortWith(Mockito.any());
    }

    @Test
    void testNoneAlgorithm() throws IOException {
        final Date now = new Date();
        final String insecureToken = Jwts.builder()
                .addClaims(Map.of("os_roles", "admin"))
                .setIssuedAt(now)
                .setSubject("admin")
                .setIssuer("graylog")
                .setNotBefore(now)
                .setExpiration(new Date(now.getTime() + Duration.seconds(10).toMilliseconds()))
                .compact();

        final String signingKey = RandomStringUtils.random(64);
        final ContainerRequestFilter fallbackFilter = Mockito.mock(ContainerRequestFilter.class);
        final DatanodeAuthFilter datanodeAuthFilter = new DatanodeAuthFilter(fallbackFilter, signingKey);

        final ContainerRequest request = mockRequest("Bearer " + insecureToken);

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
