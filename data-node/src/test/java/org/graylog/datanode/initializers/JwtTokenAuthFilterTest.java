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
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.server.ContainerRequest;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class JwtTokenAuthFilterTest {

    private static ContainerRequest mockRequest(String bearerToken) {
        final ContainerRequest request = mock(ContainerRequest.class);
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        Optional.ofNullable(bearerToken).ifPresent(token -> headers.put("Authorization", Collections.singletonList(token)));
        Mockito.when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    @Test
    void verifyValidToken() throws IOException {
        final String key = "gTVfiF6A0pB70A3UP1EahpoR6LId9DdNadIkYNygK5Z8lpeJIpw9vN0jZ6fdsfeuV9KIg9gVLkCHIPj6FHW5Q9AvpOoGZO3h";
        final JwtTokenAuthFilter validator = new JwtTokenAuthFilter(key);
        final ContainerRequest mockedRequest = mockRequest("Bearer " + generateToken(key));
        validator.filter(mockedRequest);
        Mockito.verify(mockedRequest, never()).abortWith(Mockito.any());
    }

    @Test
    void verifyNoHeaderProvided() throws IOException {
        final String key = "gTVfiF6A0pB70A3UP1EahpoR6LId9DdNadIkYNygK5Z8lpeJIpw9vN0jZ6fdsfeuV9KIg9gVLkCHIPj6FHW5Q9AvpOoGZO3h";
        final JwtTokenAuthFilter validator = new JwtTokenAuthFilter(key);
        final ContainerRequest mockedRequest = mockRequest(null);
        validator.filter(mockedRequest);
        Mockito.verify(mockedRequest, atLeastOnce()).abortWith(Mockito.any());
    }

    @Test
    void verifyInvalidToken() throws IOException {
        final String generationKey = "gTVfiF6A0pB70A3UP1EahpoR6LId9DdNadIkYNygK5Z8lpeJIpw9vN0jZ6fdsfeuV9KIg9gVLkCHIPj6FHW5Q9AvpOoGZO3h";
        final String verificationKey = "n51wcO3jn8w3JNyGgKc7k1fTCr1FWvGg7ODfQOyBT2fizBrCVsRJg2GsbYGLNejfi3QsKaqJgo3zAWMuAZhJznuizHZpv92S";
        final JwtTokenAuthFilter validator = new JwtTokenAuthFilter(verificationKey);

        final ContainerRequest mockedRequest = mockRequest("Bearer " + generateToken(generationKey));
        validator.filter(mockedRequest);
        Mockito.verify(mockedRequest, atLeastOnce()).abortWith(Mockito.any());
    }

    @Test
    void testNoneAlgorithm() {
        final String key = "gTVfiF6A0pB70A3UP1EahpoR6LId9DdNadIkYNygK5Z8lpeJIpw9vN0jZ6fdsfeuV9KIg9gVLkCHIPj6FHW5Q9AvpOoGZO3h";
        final JwtTokenAuthFilter validator = new JwtTokenAuthFilter(key);
        Assertions.assertThatThrownBy(() -> validator.verifyToken(removeSignature(generateToken(key))))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("Token format/configuration is not supported");
    }

    private String removeSignature(String token) {
        final String header = Base64.getEncoder()
                .encodeToString("{\"alg\": \"none\"}"
                        .getBytes(StandardCharsets.UTF_8));

        return header + token.substring(token.indexOf('.'), token.lastIndexOf('.') + 1);
    }

    @Nonnull
    private static String generateToken(String signingKey) {
        return IndexerJwtAuthTokenProvider.createToken(signingKey.getBytes(StandardCharsets.UTF_8), Duration.seconds(180));
    }
}
