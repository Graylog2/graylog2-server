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
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog.collectors.opamp.rest.EnrollmentTokenResponse;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.KeyUtils;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.threeten.extra.MutableClock;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnrollmentTokenService}.
 */
@ExtendWith(MongoDBExtension.class)
class EnrollmentTokenServiceTest {

    private static final String TEST_CLUSTER_ID = "test-cluster-id-12345";
    private static final String TEST_TOKEN_NAME = "test-token";
    private static final EnrollmentTokenCreator TEST_CREATOR = new EnrollmentTokenCreator("test-user-id", "testuser");

    private EnrollmentTokenService enrollmentTokenService;
    private CollectorsConfigService collectorsConfigService;
    private final MutableClock mutableClock = TestClocks.mutableFixedEpoch();
    private ClusterIdService clusterIdService;
    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");

    @BeforeEach
    void setUp(MongoDBTestService mongodb) throws Exception {
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
        clusterIdService = mock(ClusterIdService.class);
        when(clusterIdService.getString()).thenReturn(TEST_CLUSTER_ID);
        collectorsConfigService = mock(CollectorsConfigService.class);
        enrollmentTokenService = new EnrollmentTokenService(clusterIdService, mutableClock, encryptedValueService, collectorsConfigService, mongoCollections);

        final var collectorsConfig = mock(CollectorsConfig.class);

        when(collectorsConfigService.get()).thenReturn(Optional.of(collectorsConfig));

        final var keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);
        when(collectorsConfig.tokenSigningKey()).thenReturn(new TokenSigningKey(
                encryptedValueService.encrypt(PemUtils.toPem(keyPair.getPrivate())),
                PemUtils.toPem(keyPair.getPublic()),
                KeyUtils.sha256Fingerprint(keyPair),
                Instant.now(mutableClock)
        ));
    }

    @Test
    void createTokenReturnsValidJwt() throws Exception {
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
                "test-fleet",
                Duration.ofDays(1)
        );

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        assertThat(response.token()).isNotNull();
        assertThat(response.expiresAt()).isEqualTo(Clock.offset(mutableClock, Duration.ofDays(1)).instant());

        // Verify JWT can be parsed and has correct claims
        final var tokenSigningKey = collectorsConfigService.get()
                .map(CollectorsConfig::tokenSigningKey)
                .orElseThrow(AssertionError::new);
        final PublicKey publicKey = PemUtils.parsePublicKey(tokenSigningKey.publicKey());

        final Jws<Claims> jws = Jwts.parser()
                .clock(() -> Date.from(mutableClock.instant()))
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(response.token());

        assertThat(jws.getPayload().getIssuer()).isEqualTo(TEST_CLUSTER_ID);
        assertThat(jws.getPayload().getAudience()).containsExactly(TEST_CLUSTER_ID + ":opamp");
        assertThat(jws.getPayload().getId()).isNotNull();
        assertThat(jws.getHeader().get("kid")).isEqualTo(tokenSigningKey.fingerprint());
    }

    @Test
    void createTokenWithNullExpiryProducesNonExpiringJwt() throws Exception {
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(TEST_TOKEN_NAME, "default-fleet", null);

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Null expiresIn means no expiry
        assertThat(response.expiresAt()).isNull();

        // Verify JWT has no exp claim
        final var tokenSigningKey = collectorsConfigService.get().map(CollectorsConfig::tokenSigningKey).orElseThrow(AssertionError::new);

        final Jws<Claims> jws = Jwts.parser()
                .verifyWith(PemUtils.parsePublicKey(tokenSigningKey.publicKey()))
                .build()
                .parseSignedClaims(response.token());

        assertThat(jws.getPayload().getExpiration()).isNull();
    }

    @Test
    void validateTokenReturnsEnrollmentForValidToken() {
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
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
        // Create token that expires in 1 second
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
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
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
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
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
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
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(TEST_TOKEN_NAME, "test-fleet", Duration.ofDays(1));
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
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
                "persist-fleet",
                Duration.ofDays(1)
        );
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request, TEST_CREATOR);

        // Validate the token to retrieve the persisted DTO
        final Optional<EnrollmentTokenDTO> result = enrollmentTokenService.validateToken(response.token());

        assertThat(result).isPresent();
        final EnrollmentTokenDTO dto = result.get();
        assertThat(dto.name()).isEqualTo(TEST_TOKEN_NAME);
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
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
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
    void createTokenSigningKeyReturnsValidKey() throws Exception {
        final TokenSigningKey signingKey = enrollmentTokenService.createTokenSigningKey();

        assertThat(signingKey).isNotNull();
        assertThat(signingKey.privateKey()).isNotNull();
        assertThat(signingKey.privateKey().isSet()).isTrue();
        assertThat(signingKey.fingerprint()).startsWith("SHA256:");
        assertThat(signingKey.createdAt()).isEqualTo(mutableClock.instant());
    }

    @Test
    void createTokenSigningKeyGeneratesUniqueKeys() throws Exception {
        final TokenSigningKey first = enrollmentTokenService.createTokenSigningKey();
        final TokenSigningKey second = enrollmentTokenService.createTokenSigningKey();

        assertThat(first.fingerprint()).isNotEqualTo(second.fingerprint());
    }

    @Test
    void createTokenSigningKeyPrivateKeyIsDecryptable() throws Exception {
        final TokenSigningKey signingKey = enrollmentTokenService.createTokenSigningKey();

        final String decryptedPem = encryptedValueService.decrypt(signingKey.privateKey());

        assertThat(decryptedPem).startsWith("-----BEGIN PRIVATE KEY-----");
        // Verify the PEM can be parsed back into a PrivateKey
        final var privateKey = PemUtils.parsePrivateKey(decryptedPem);
        assertThat(privateKey).isNotNull();
        // BouncyCastle's PEM parser returns "EdDSA" as the algorithm family,
        // while the original KeyPairGenerator("Ed25519") reports "Ed25519"
        assertThat(privateKey.getAlgorithm()).isIn("Ed25519", "EdDSA");
    }

    @Test
    void deleteManyRemovesMultipleTokens() {
        final var request1 = new CreateEnrollmentTokenRequest(TEST_TOKEN_NAME, "fleet-1", Duration.ofDays(1));
        final var request2 = new CreateEnrollmentTokenRequest("token-2", "fleet-1", Duration.ofDays(1));
        final var request3 = new CreateEnrollmentTokenRequest("token-3", "fleet-2", Duration.ofDays(1));

        final var response1 = enrollmentTokenService.createToken(request1, TEST_CREATOR);
        final var response2 = enrollmentTokenService.createToken(request2, TEST_CREATOR);
        final var response3 = enrollmentTokenService.createToken(request3, TEST_CREATOR);

        final var dto1 = enrollmentTokenService.validateToken(response1.token()).orElseThrow();
        final var dto2 = enrollmentTokenService.validateToken(response2.token()).orElseThrow();
        final var dto3 = enrollmentTokenService.validateToken(response3.token()).orElseThrow();

        // Delete tokens 1 and 2
        final long deleted = enrollmentTokenService.deleteMany(List.of(dto1.id(), dto2.id()));
        assertThat(deleted).isEqualTo(2);

        // Token 3 should still be valid
        assertThat(enrollmentTokenService.validateToken(response3.token())).isPresent();
        // Tokens 1 and 2 should be gone
        assertThat(enrollmentTokenService.validateToken(response1.token())).isEmpty();
        assertThat(enrollmentTokenService.validateToken(response2.token())).isEmpty();
    }

    @Test
    void deleteManyReturnsZeroForNonexistentIds() {
        final long deleted = enrollmentTokenService.deleteMany(List.of("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012"));
        assertThat(deleted).isEqualTo(0);
    }

    @Test
    void getStatsReturnsZerosForEmptyCollection() {
        final var stats = enrollmentTokenService.getStats();

        assertThat(stats.count()).isEqualTo(0);
        assertThat(stats.totalUsage()).isEqualTo(0);
        assertThat(stats.expired()).isEqualTo(0);
    }

    @Test
    void getStatsReturnsCorrectCountsAndUsage() {
        // Create 3 tokens, increment usage on two of them
        final var t1 = enrollmentTokenService.createToken(
                new CreateEnrollmentTokenRequest("token-1", "fleet-1", Duration.ofDays(1)), TEST_CREATOR);
        final var t2 = enrollmentTokenService.createToken(
                new CreateEnrollmentTokenRequest("token-2", "fleet-1", Duration.ofDays(1)), TEST_CREATOR);
        enrollmentTokenService.createToken(
                new CreateEnrollmentTokenRequest("token-3", "fleet-2", null), TEST_CREATOR);

        enrollmentTokenService.incrementUsage(t1.id());
        enrollmentTokenService.incrementUsage(t1.id());
        enrollmentTokenService.incrementUsage(t2.id());

        final var stats = enrollmentTokenService.getStats();

        assertThat(stats.count()).isEqualTo(3);
        assertThat(stats.totalUsage()).isEqualTo(3);
        assertThat(stats.expired()).isEqualTo(0);
    }

    @Test
    void getStatsCountsExpiredTokens() {
        // Token that expires in 1 hour
        enrollmentTokenService.createToken(
                new CreateEnrollmentTokenRequest("expires-soon", "fleet-1", Duration.ofHours(1)), TEST_CREATOR);
        // Token that expires in 2 days
        enrollmentTokenService.createToken(
                new CreateEnrollmentTokenRequest("expires-later", "fleet-1", Duration.ofDays(2)), TEST_CREATOR);
        // Token that never expires
        enrollmentTokenService.createToken(
                new CreateEnrollmentTokenRequest("no-expiry", "fleet-1", null), TEST_CREATOR);

        // Advance clock by 2 hours — first token should be expired
        mutableClock.add(Duration.ofHours(2));

        final var stats = enrollmentTokenService.getStats();

        assertThat(stats.count()).isEqualTo(3);
        assertThat(stats.expired()).isEqualTo(1);

        // Advance clock by 2 more days — second token also expired now
        mutableClock.add(Duration.ofDays(2));

        final var stats2 = enrollmentTokenService.getStats();

        assertThat(stats2.count()).isEqualTo(3);
        assertThat(stats2.expired()).isEqualTo(2);
    }

    @Test
    void incrementUsageUpdatesCountAndLastUsed() {
        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest(
                TEST_TOKEN_NAME,
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
