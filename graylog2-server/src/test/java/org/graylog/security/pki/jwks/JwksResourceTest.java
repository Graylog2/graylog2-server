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
package org.graylog.security.pki.jwks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JwksResource}.
 */
@ExtendWith(MockitoExtension.class)
class JwksResourceTest {

    @Mock
    private JwksService jwksService;

    private JwksResource resource;

    @BeforeEach
    void setUp() {
        resource = new JwksResource(jwksService);
    }

    @Test
    void getJwksDelegatesToService() {
        final JwksResponse expectedResponse = new JwksResponse(List.of());
        when(jwksService.getJwks()).thenReturn(expectedResponse);

        final JwksResponse result = resource.getJwks();

        assertThat(result).isSameAs(expectedResponse);
        verify(jwksService).getJwks();
    }

    @Test
    void getJwksReturnsKeysFromService() {
        final OkpJwk jwk = new OkpJwk("sha256:abc123", "Ed25519", "base64urlPublicKey", "sig");
        final JwksResponse expectedResponse = new JwksResponse(List.of(jwk));
        when(jwksService.getJwks()).thenReturn(expectedResponse);

        final JwksResponse result = resource.getJwks();

        assertThat(result.keys()).hasSize(1);
        assertThat(result.keys().get(0).kid()).isEqualTo("sha256:abc123");
    }
}
