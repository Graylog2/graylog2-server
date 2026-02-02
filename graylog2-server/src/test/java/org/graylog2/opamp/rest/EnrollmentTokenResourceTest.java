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
package org.graylog2.opamp.rest;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.opamp.enrollment.EnrollmentTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentTokenResourceTest {

    private static final URI TEST_EXTERNAL_URI = URI.create("https://graylog.example.com/");

    private EnrollmentTokenResource resource;
    private EnrollmentTokenService enrollmentTokenService;
    private HttpHeaders httpHeaders;

    @BeforeEach
    void setUp() {
        enrollmentTokenService = mock(EnrollmentTokenService.class);

        final HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(TEST_EXTERNAL_URI);

        httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());

        resource = new EnrollmentTokenResource(enrollmentTokenService, httpConfiguration);
    }

    @Test
    void createTokenDelegatesToService() {
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse expectedResponse = new EnrollmentTokenResponse(
                "test-token",
                Instant.now().plusSeconds(86400)
        );

        when(enrollmentTokenService.createToken(any(), any())).thenReturn(expectedResponse);

        final EnrollmentTokenResponse response = resource.createToken(httpHeaders, request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(enrollmentTokenService).createToken(eq(request), eq(TEST_EXTERNAL_URI));
    }
}
