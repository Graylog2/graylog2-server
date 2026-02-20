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

import jakarta.ws.rs.BadRequestException;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog2.opamp.enrollment.EnrollmentTokenService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentTokenResourceTest {

    private EnrollmentTokenResource resource;
    private EnrollmentTokenService enrollmentTokenService;
    private ClusterConfigService clusterConfigService;

    @BeforeEach
    void setUp() {
        enrollmentTokenService = mock(EnrollmentTokenService.class);
        clusterConfigService = mock(ClusterConfigService.class);
        resource = new EnrollmentTokenResource(enrollmentTokenService, clusterConfigService);
    }

    @Test
    void createTokenDelegatesToService() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(
                new CollectorsConfig("ca-id", "token-id", "otlp-id",
                        new IngestEndpointConfig(true, "host", 14401, "input-1"),
                        new IngestEndpointConfig(false, "host", 14402, null)));

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
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );

        assertThatThrownBy(() -> resource.createToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Collectors must be configured");
    }
}
