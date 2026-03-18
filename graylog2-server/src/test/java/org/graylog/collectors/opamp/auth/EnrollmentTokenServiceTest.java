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
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.OpAmpCaService;
import org.graylog.collectors.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog.collectors.opamp.rest.EnrollmentTokenResponse;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.threeten.extra.MutableClock;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnrollmentTokenService}.
 */
@ExtendWith(MongoDBExtension.class)
class EnrollmentTokenServiceTest {

    private static final String TEST_CLUSTER_ID = "test-cluster-id-12345";
    private static final EnrollmentTokenCreator TEST_CREATOR = new EnrollmentTokenCreator("test-user-id", "testuser");

    private EnrollmentTokenService enrollmentTokenService;
    private CollectorsConfigService collectorsConfigService;
    private final MutableClock mutableClock = TestClocks.mutableFixedEpoch();
    private ClusterIdService clusterIdService;

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
        final CertificateService certificateService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty(), mutableClock);
        clusterIdService = mock(ClusterIdService.class);
        when(clusterIdService.getString()).thenReturn(TEST_CLUSTER_ID);
        collectorsConfigService = mock(CollectorsConfigService.class);
        final OpAmpCaService opAmpCaService = new OpAmpCaService(certificateService, clusterIdService, collectorsConfigService);
        enrollmentTokenService = new EnrollmentTokenService(certificateService, clusterIdService, opAmpCaService, mutableClock, mongoCollections);
    }

    @Test
    void getTokenSigningCertCreatesHierarchyOnFirstCall() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

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
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        // First call - no config, creates hierarchy
        final CertificateEntry firstCert = enrollmentTokenService.getTokenSigningCert();

        // Second call - cached hierarchy, should return same cert
        final CertificateEntry secondCert = enrollmentTokenService.getTokenSigningCert();

        assertThat(secondCert.id()).isEqualTo(firstCert.id());
        assertThat(secondCert.fingerprint()).isEqualTo(firstCert.fingerprint());
    }

    @Test
    void getEnrollmentSigningCertCreatesHierarchyOnFirstCall() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CertificateEntry cert = enrollmentTokenService.getEnrollmentSigningCert();

        assertThat(cert).isNotNull();
        assertThat(cert.id()).isNotNull();
        assertThat(cert.fingerprint()).startsWith("sha256:");

        // Enrollment CA should have issuer chain (root CA only)
        assertThat(cert.issuerChain()).hasSize(1);
    }

    @Test
    void getEnrollmentSigningCertUsesExistingConfigOnSubsequentCalls() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        // First call - no config, creates hierarchy
        final CertificateEntry firstCert = enrollmentTokenService.getEnrollmentSigningCert();

        // Second call - cached hierarchy, should return same cert
        final CertificateEntry secondCert = enrollmentTokenService.getEnrollmentSigningCert();

        assertThat(secondCert.id()).isEqualTo(firstCert.id());
        assertThat(secondCert.fingerprint()).isEqualTo(firstCert.fingerprint());
    }

    @Test
    void createTokenReturnsValidJwt() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        assertThat(response.token()).isNotNull();
        assertThat(response.expiresAt()).isEqualTo(Clock.offset(mutableClock, Duration.ofDays(1)).instant());

        // Verify JWT can be parsed and has correct claims
        final CertificateEntry signingCert = enrollmentTokenService.getTokenSigningCert();
        final X509Certificate cert = PemUtils.parseCertificate(signingCert.certificate());
        final PublicKey publicKey = cert.getPublicKey();

        final Jws<Claims> jws = Jwts.parser()
                .clock(() -> Date.from(mutableClock.instant()))
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(response.token());

        assertThat(jws.getPayload().getIssuer()).isEqualTo(TEST_CLUSTER_ID);
        assertThat(jws.getPayload().getAudience()).containsExactly(TEST_CLUSTER_ID + ":opamp");
        assertThat(jws.getPayload().getId()).isNotNull();
        assertThat(jws.getHeader().get("kid")).isEqualTo(signingCert.fingerprint());
    }

    @Test
    void createTokenWithNullExpiryProducesNonExpiringJwt() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest("default-fleet", null);

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Null expiresIn means no expiry
        assertThat(response.expiresAt()).isNull();

        // Verify JWT has no exp claim
        final CertificateEntry signingCert = enrollmentTokenService.getTokenSigningCert();
        final X509Certificate cert = PemUtils.parseCertificate(signingCert.certificate());
        final PublicKey publicKey = cert.getPublicKey();

        final Jws<Claims> jws = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(response.token());

        assertThat(jws.getPayload().getExpiration()).isNull();
    }

    @Test
    void createTokenRejectsExpiryBeyondCertValidity() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        // Token signing cert is valid for 2 years, so 3 years should fail
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(3 * 365)
        );

        assertThatThrownBy(() -> enrollmentTokenService.createToken(request, TEST_CREATOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds signing certificate expiry");
    }

    @Test
    void validateTokenReturnsEnrollmentForValidToken() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        final Optional<EnrollmentTokenDTO> result = enrollmentTokenService.validateToken(response.token());

        assertThat(result).isPresent();
        assertThat(result.get().fleetId()).isEqualTo("test-fleet");
    }

    @Test
    void validateTokenReturnsEmptyForExpiredToken() throws Exception {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        // Create token that expires in 1 second
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofSeconds(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Wait for expiry
        mutableClock.add(Duration.ofSeconds(2));

        final Optional<EnrollmentTokenDTO> result = enrollmentTokenService.validateToken(response.token());

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenReturnsEmptyForWrongClusterId() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Switch cluster ID so validation audience check fails
        when(clusterIdService.getString()).thenReturn("different-cluster-id");

        final Optional<EnrollmentTokenDTO> result = enrollmentTokenService.validateToken(response.token());

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenReturnsEmptyForInvalidSignature() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Tamper with the signature (last part of JWT)
        final String tamperedToken = response.token().substring(0, response.token().lastIndexOf('.') + 1) + "invalid";

        final Optional<EnrollmentTokenDTO> result = enrollmentTokenService.validateToken(tamperedToken);

        assertThat(result).isEmpty();
    }

    @Test
    void createTokenIncludesCttHeader() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest("test-fleet", Duration.ofDays(1));
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Decode header without verification
        final String[] parts = response.token().split("\\.");
        final String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);

        assertThat(headerJson).contains("\"ctt\":\"enrollment\"");
    }

    @Test
    void validateTokenReturnsEmptyForUnknownKid() {
        // Create a valid token structure but with unknown kid
        // This is a token with header {"alg":"EdDSA","kid":"sha256:unknown"}
        final String fakeToken = "eyJhbGciOiJFZERTQSIsImtpZCI6InNoYTI1Njp1bmtub3duIn0.eyJpc3MiOiJodHRwczovL2dyYXlsb2cuZXhhbXBsZS5jb20vIn0.invalidsig";

        final Optional<EnrollmentTokenDTO> result = enrollmentTokenService.validateToken(fakeToken);

        assertThat(result).isEmpty();
    }

    @Test
    void createTokenPersistsMetadata() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "persist-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Validate the token to retrieve the persisted DTO
        final Optional<EnrollmentTokenDTO> result = enrollmentTokenService.validateToken(response.token());

        assertThat(result).isPresent();
        final EnrollmentTokenDTO dto = result.get();
        assertThat(dto.fleetId()).isEqualTo("persist-fleet");
        assertThat(dto.createdBy()).isEqualTo(TEST_CREATOR);
        assertThat(dto.jti()).isNotNull();
        assertThat(dto.kid()).isNotNull();
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.usageCount()).isEqualTo(0);
        assertThat(dto.lastUsedAt()).isNull();
    }

    @Test
    void validateTokenRejectsDeletedToken() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "delete-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Validate first to get the DTO with its ID
        final Optional<EnrollmentTokenDTO> beforeDelete = enrollmentTokenService.validateToken(response.token());
        assertThat(beforeDelete).isPresent();

        // Delete the token
        final boolean deleted = enrollmentTokenService.delete(beforeDelete.get().id());
        assertThat(deleted).isTrue();

        // Try to validate again - should be empty since metadata was deleted
        final Optional<EnrollmentTokenDTO> afterDelete = enrollmentTokenService.validateToken(response.token());
        assertThat(afterDelete).isEmpty();
    }

    @Test
    void incrementUsageUpdatesCountAndLastUsed() {
        // No existing CollectorsConfig - service will create certs and cache in memory
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                "usage-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Validate to get the DTO
        final Optional<EnrollmentTokenDTO> initial = enrollmentTokenService.validateToken(response.token());
        assertThat(initial).isPresent();
        assertThat(initial.get().usageCount()).isEqualTo(0);
        assertThat(initial.get().lastUsedAt()).isNull();

        // Increment usage
        enrollmentTokenService.incrementUsage(initial.get().id());

        // Validate again to get the updated DTO
        final Optional<EnrollmentTokenDTO> updated = enrollmentTokenService.validateToken(response.token());
        assertThat(updated).isPresent();
        assertThat(updated.get().usageCount()).isEqualTo(1);
        assertThat(updated.get().lastUsedAt()).isNotNull();
    }
}
