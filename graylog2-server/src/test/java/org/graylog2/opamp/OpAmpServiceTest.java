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
package org.graylog2.opamp;

import org.graylog.security.certificates.CertificateService;
import org.graylog2.opamp.enrollment.EnrollmentTokenService;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OpAmpService} authentication dispatch logic.
 */
@ExtendWith(MockitoExtension.class)
class OpAmpServiceTest {

    @Mock
    private EnrollmentTokenService enrollmentTokenService;

    @Mock
    private OpAmpAgentService agentService;

    @Mock
    private CertificateService certificateService;

    private OpAmpService opAmpService;

    private static final URI EXTERNAL_URI = URI.create("https://graylog.example.com/");
    private static final OpAmpAuthContext.Transport TRANSPORT = OpAmpAuthContext.Transport.HTTP;

    @BeforeEach
    void setUp() {
        opAmpService = new OpAmpService(enrollmentTokenService, agentService, certificateService);
    }

    @Test
    void authenticateDispatchesEnrollmentTokenByCttHeader() {
        // Create a token with ctt: enrollment
        final String token = createTokenWithCtt("enrollment");
        final String authHeader = "Bearer " + token;
        final OpAmpAuthContext.Enrollment expectedContext = new OpAmpAuthContext.Enrollment("test-fleet", TRANSPORT);

        when(enrollmentTokenService.validateToken(eq(token), eq(EXTERNAL_URI), eq(TRANSPORT)))
                .thenReturn(Optional.of(expectedContext));

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(OpAmpAuthContext.Enrollment.class);
        verify(enrollmentTokenService).validateToken(token, EXTERNAL_URI, TRANSPORT);
        verify(enrollmentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateDispatchesAgentTokenByCttHeader() {
        // Create a token with ctt: agent
        final String token = createTokenWithCtt("agent");
        final String authHeader = "Bearer " + token;
        final OpAmpAgent agent = new OpAmpAgent(
                "test-id",
                "instance-uid",
                "fleet-id",
                "sha256:fingerprint",
                "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
                "507f1f77bcf86cd799439011",
                Instant.now()
        );
        final OpAmpAuthContext.Identified expectedContext = new OpAmpAuthContext.Identified(agent, TRANSPORT);

        when(enrollmentTokenService.validateAgentToken(eq(token), eq(TRANSPORT)))
                .thenReturn(Optional.of(expectedContext));

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(OpAmpAuthContext.Identified.class);
        verify(enrollmentTokenService).validateAgentToken(token, TRANSPORT);
        verify(enrollmentTokenService, never()).validateToken(any(), any(), any());
    }

    @Test
    void authenticateReturnsEmptyForUnknownCttHeader() {
        // Create a token with unknown ctt
        final String token = createTokenWithCtt("unknown");
        final String authHeader = "Bearer " + token;

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any(), any(), any());
        verify(enrollmentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForMissingCttHeader() {
        // Create a token without ctt header
        final String token = createTokenWithoutCtt();
        final String authHeader = "Bearer " + token;

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any(), any(), any());
        verify(enrollmentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForMissingBearer() {
        final String authHeader = "Basic dXNlcjpwYXNz";

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any(), any(), any());
        verify(enrollmentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForNullHeader() {
        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(null, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any(), any(), any());
        verify(enrollmentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForMalformedToken() {
        final String authHeader = "Bearer not.a.valid.jwt";

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any(), any(), any());
        verify(enrollmentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForInvalidBase64() {
        final String authHeader = "Bearer !!!invalid!!!.payload.signature";

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, EXTERNAL_URI, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any(), any(), any());
        verify(enrollmentTokenService, never()).validateAgentToken(any(), any());
    }

    /**
     * Creates a JWT-like token with the specified ctt (custom token type) header.
     * This is not a valid signed JWT, just has the correct structure for header parsing.
     */
    private String createTokenWithCtt(String ctt) {
        final String header = f("{\"alg\":\"EdDSA\",\"ctt\":\"%s\",\"kid\":\"fingerprint\"}", ctt);
        final String payload = "{\"sub\":\"test\",\"exp\":9999999999}";
        final String signature = "signature";

        return Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;
    }

    /**
     * Creates a JWT-like token without a ctt header.
     */
    private String createTokenWithoutCtt() {
        final String header = "{\"alg\":\"EdDSA\",\"kid\":\"fingerprint\"}";
        final String payload = "{\"sub\":\"test\",\"exp\":9999999999}";
        final String signature = "signature";

        return Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;
    }
}
