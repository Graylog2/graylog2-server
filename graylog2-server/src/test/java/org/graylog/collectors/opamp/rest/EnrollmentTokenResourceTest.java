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
package org.graylog.collectors.opamp.rest;

import jakarta.ws.rs.BadRequestException;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentTokenResourceTest {

    private EnrollmentTokenResource resource;
    private EnrollmentTokenService enrollmentTokenService;
    private CollectorsConfigService collectorsConfigService;

    @BeforeEach
    void setUp() {
        enrollmentTokenService = mock(EnrollmentTokenService.class);
        collectorsConfigService = mock(CollectorsConfigService.class);
        resource = new EnrollmentTokenResource(enrollmentTokenService, collectorsConfigService);
    }

    @Test
    void createTokenDelegatesToService() {
        when(collectorsConfigService.get()).thenReturn(Optional.of(
                CollectorsConfig.createDefaultBuilder("host")
                        .caCertId("ca-cert-id")
                        .signingCertId("signing-cert-id")
                        .tokenSigningCertId("token-id")
                        .otlpServerCertId("otlp-id")
                        .build()));

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse expectedResponse = new EnrollmentTokenResponse(
                "test-token",
                Instant.now().plusSeconds(86400)
        );

        when(enrollmentTokenService.createToken(any(CreateEnrollmentTokenRequest.class))).thenReturn(expectedResponse);

        final EnrollmentTokenResponse response = resource.createToken(request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(enrollmentTokenService).createToken(request);
    }

    @Test
    void createTokenThrowsWhenCollectorsNotConfigured() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );

        assertThatThrownBy(() -> resource.createToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Collectors must be configured");
    }
}
