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

import org.graylog2.opamp.enrollment.EnrollmentTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentTokenResourceTest {

    private EnrollmentTokenResource resource;
    private EnrollmentTokenService enrollmentTokenService;

    @BeforeEach
    void setUp() {
        enrollmentTokenService = mock(EnrollmentTokenService.class);
        resource = new EnrollmentTokenResource(enrollmentTokenService);
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

        when(enrollmentTokenService.createToken(any(CreateEnrollmentTokenRequest.class))).thenReturn(expectedResponse);

        final EnrollmentTokenResponse response = resource.createToken(request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(enrollmentTokenService).createToken(request);
    }
}
