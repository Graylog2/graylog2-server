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
package org.graylog.collectors.opamp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.opamp.OpAmpCaService;
import org.graylog.collectors.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog.collectors.opamp.rest.EnrollmentTokenResponse;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnrollmentTokenService}.
 */
@ExtendWith(MongoDBExtension.class)
class EnrollmentTokenServiceTest {

    private static final String TEST_CLUSTER_ID = "test-cluster-id-12345";

    private ClusterConfigService clusterConfigService;
    private EnrollmentTokenService enrollmentTokenService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
        final ObjectMapper objectMapper = new ObjectMapperProvider(
                ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                encryptedValueService,
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig()
        ).get();

        final MongoCollections mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper),
                mongodb.mongoConnection()
        );
        final CertificateService certificateService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty());
        clusterConfigService = mock(ClusterConfigService.class);
        when(clusterConfigService.get(ClusterId.class))
                .thenReturn(ClusterId.create(TEST_CLUSTER_ID));
        final OpAmpCaService opAmpCaService = new OpAmpCaService(certificateService, clusterConfigService);
        enrollmentTokenService = new EnrollmentTokenService(certificateService, clusterConfigService, opAmpCaService);
    }

    @Test
    void getTokenSigningCertCreatesHierarchyOnFirstCall() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CertificateEntry cert = enrollmentTokenService.getTokenSigningCert();

        assertThat(cert).isNotNull();
        assertThat(cert.id()).isNotNull();
        assertThat(cert.fingerprint()).startsWith("sha256:");
        assertThat(cert.certificate()).startsWith("-----BEGIN CERTIFICATE-----");

        // Token signing cert should have issuer chain (enrollment CA + root CA)
        assertThat(cert.issuerChain()).hasSize(2);
    }

    @Test
    void getTokenSigningCertUsesExistingConfigOnSubsequentCalls() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // First call - no config, creates hierarchy
        final CertificateEntry firstCert = enrollmentTokenService.getTokenSigningCert();

        // Second call - cached hierarchy, should return same cert
        final CertificateEntry secondCert = enrollmentTokenService.getTokenSigningCert();

        assertThat(secondCert.id()).isEqualTo(firstCert.id());
        assertThat(secondCert.fingerprint()).isEqualTo(firstCert.fingerprint());
    }

    @Test
    void getEnrollmentCaCreatesHierarchyOnFirstCall() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CertificateEntry cert = enrollmentTokenService.getEnrollmentCa();

        assertThat(cert).isNotNull();
        assertThat(cert.id()).isNotNull();
        assertThat(cert.fingerprint()).startsWith("sha256:");

        // Enrollment CA should have issuer chain (root CA only)
        assertThat(cert.issuerChain()).hasSize(1);
    }

    @Test
    void getEnrollmentCaUsesExistingConfigOnSubsequentCalls() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // First call - no config, creates hierarchy
        final CertificateEntry firstCert = enrollmentTokenService.getEnrollmentCa();

        // Second call - cached hierarchy, should return same cert
        final CertificateEntry secondCert = enrollmentTokenService.getEnrollmentCa();

        assertThat(secondCert.id()).isEqualTo(firstCert.id());
        assertThat(secondCert.fingerprint()).isEqualTo(firstCert.fingerprint());
    }

    @Test
    void createTokenReturnsValidJwt() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        assertThat(response.token()).isNotNull();
        assertThat(response.expiresAt()).isAfter(Instant.now());
        assertThat(response.expiresAt()).isBefore(Instant.now().plus(2, ChronoUnit.DAYS));

        // Verify JWT can be parsed and has correct claims
        final CertificateEntry signingCert = enrollmentTokenService.getTokenSigningCert();
        final X509Certificate cert = PemUtils.parseCertificate(signingCert.certificate());
        final PublicKey publicKey = cert.getPublicKey();

        final Jws<Claims> jws = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(response.token());

        assertThat(jws.getPayload().getIssuer()).isEqualTo(TEST_CLUSTER_ID);
        assertThat(jws.getPayload().getAudience()).containsExactly(TEST_CLUSTER_ID + ":opamp");
        assertThat(jws.getPayload().get("fleet_id", String.class)).isEqualTo("test-fleet");
        assertThat(jws.getHeader().get("kid")).isEqualTo(signingCert.fingerprint());
    }

    @Test
    void createTokenUsesDefaultExpiryWhenNotSpecified() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest("default-fleet", null);

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        // Default is 7 days
        final Instant expectedExpiry = Instant.now().plus(CreateEnrollmentTokenRequest.DEFAULT_EXPIRY);
        assertThat(response.expiresAt()).isCloseTo(expectedExpiry, within(1, ChronoUnit.MINUTES));
    }

    @Test
    void createTokenRejectsExpiryBeyondCertValidity() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // Token signing cert is valid for 2 years, so 3 years should fail
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(3 * 365)
        );

        assertThatThrownBy(() -> enrollmentTokenService.createToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds signing certificate expiry");
    }

    @Test
    void validateTokenReturnsEnrollmentForValidToken() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        final Optional<OpAmpAuthContext.Enrollment> result = enrollmentTokenService.validateToken(
                response.token(), OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isPresent();
        assertThat(result.get().fleetId()).isEqualTo("test-fleet");
        assertThat(result.get().transport()).isEqualTo(OpAmpAuthContext.Transport.HTTP);
    }

    @Test
    void validateTokenReturnsEmptyForExpiredToken() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // Create token that expires in 1 second
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofSeconds(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        // Wait for expiry
        Thread.sleep(1500);

        final Optional<OpAmpAuthContext.Enrollment> result = enrollmentTokenService.validateToken(
                response.token(), OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenReturnsEmptyForWrongClusterId() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        // Switch cluster ID so validation audience check fails
        when(clusterConfigService.get(ClusterId.class))
                .thenReturn(ClusterId.create("different-cluster-id"));

        final Optional<OpAmpAuthContext.Enrollment> result = enrollmentTokenService.validateToken(
                response.token(), OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenReturnsEmptyForInvalidSignature() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        // Tamper with the signature (last part of JWT)
        final String tamperedToken = response.token().substring(0, response.token().lastIndexOf('.') + 1) + "invalid";

        final Optional<OpAmpAuthContext.Enrollment> result = enrollmentTokenService.validateToken(
                tamperedToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void createTokenIncludesCttHeader() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest("test-fleet", Duration.ofDays(1));
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        // Decode header without verification
        final String[] parts = response.token().split("\\.");
        final String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);

        assertThat(headerJson).contains("\"ctt\":\"enrollment\"");
    }

    @Test
    void validateTokenReturnsEmptyForUnknownKid() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        // Create a valid token structure but with unknown kid
        // This is a token with header {"alg":"EdDSA","kid":"sha256:unknown"}
        final String fakeToken = "eyJhbGciOiJFZERTQSIsImtpZCI6InNoYTI1Njp1bmtub3duIn0.eyJpc3MiOiJodHRwczovL2dyYXlsb2cuZXhhbXBsZS5jb20vIn0.invalidsig";

        final Optional<OpAmpAuthContext.Enrollment> result = enrollmentTokenService.validateToken(
                fakeToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }
}
