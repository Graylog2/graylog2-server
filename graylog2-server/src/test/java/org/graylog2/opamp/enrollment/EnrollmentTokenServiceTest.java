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
package org.graylog2.opamp.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.opamp.config.OpAmpCaConfig;
import org.graylog2.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog2.opamp.rest.EnrollmentTokenResponse;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnrollmentTokenService}.
 */
@ExtendWith(MongoDBExtension.class)
class EnrollmentTokenServiceTest {

    private static final String TEST_CLUSTER_ID = "test-cluster-id-12345";

    private CertificateService certificateService;
    private ClusterConfigService clusterConfigService;
    private EnrollmentTokenService enrollmentTokenService;
    private CollectorInstanceService collectorInstanceService;

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
        certificateService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty());
        clusterConfigService = mock(ClusterConfigService.class);
        when(clusterConfigService.get(ClusterId.class))
                .thenReturn(ClusterId.create(TEST_CLUSTER_ID));
        collectorInstanceService = new CollectorInstanceService(mongoCollections);
        enrollmentTokenService = new EnrollmentTokenService(certificateService, clusterConfigService, collectorInstanceService);
    }

    @Test
    void getTokenSigningCertCreatesHierarchyOnFirstCall() {
        // Mock ClusterConfigService to track written config
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        final CertificateEntry cert = enrollmentTokenService.getTokenSigningCert();

        assertThat(cert).isNotNull();
        assertThat(cert.id()).isNotNull();
        assertThat(cert.fingerprint()).startsWith("sha256:");
        assertThat(cert.certificate()).startsWith("-----BEGIN CERTIFICATE-----");

        // Token signing cert should have issuer chain (enrollment CA + root CA)
        assertThat(cert.issuerChain()).hasSize(2);

        // Verify config was written
        verify(clusterConfigService).write(any(OpAmpCaConfig.class));
    }

    @Test
    void getTokenSigningCertUsesExistingConfigOnSubsequentCalls() {
        // Mock ClusterConfigService to track written config
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        // First call - no config, creates hierarchy
        final CertificateEntry firstCert = enrollmentTokenService.getTokenSigningCert();

        // Second call - config exists, should return same cert
        final CertificateEntry secondCert = enrollmentTokenService.getTokenSigningCert();

        assertThat(secondCert.id()).isEqualTo(firstCert.id());
        assertThat(secondCert.fingerprint()).isEqualTo(firstCert.fingerprint());

        // Config was only written once (during first call)
        verify(clusterConfigService, times(1)).write(any(OpAmpCaConfig.class));
    }

    @Test
    void getEnrollmentCaCreatesHierarchyOnFirstCall() {
        // Mock ClusterConfigService to track written config
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        final CertificateEntry cert = enrollmentTokenService.getEnrollmentCa();

        assertThat(cert).isNotNull();
        assertThat(cert.id()).isNotNull();
        assertThat(cert.fingerprint()).startsWith("sha256:");

        // Enrollment CA should have issuer chain (root CA only)
        assertThat(cert.issuerChain()).hasSize(1);

        // Verify config was written
        verify(clusterConfigService).write(any(OpAmpCaConfig.class));
    }

    @Test
    void getEnrollmentCaUsesExistingConfigOnSubsequentCalls() {
        // Mock ClusterConfigService to track written config
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        // First call - no config, creates hierarchy
        final CertificateEntry firstCert = enrollmentTokenService.getEnrollmentCa();

        // Second call - config exists, should return same cert
        final CertificateEntry secondCert = enrollmentTokenService.getEnrollmentCa();

        assertThat(secondCert.id()).isEqualTo(firstCert.id());
        assertThat(secondCert.fingerprint()).isEqualTo(firstCert.fingerprint());

        // Config was only written once (during first call)
        verify(clusterConfigService, times(1)).write(any(OpAmpCaConfig.class));
    }

    @Test
    void createTokenReturnsValidJwt() throws Exception {
        // Mock ClusterConfigService to track written config
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

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
    void createTokenUsesDefaultExpiryWhenNotSpecified() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest("default-fleet", null);

        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        // Default is 7 days
        final Instant expectedExpiry = Instant.now().plus(CreateEnrollmentTokenRequest.DEFAULT_EXPIRY);
        assertThat(response.expiresAt()).isCloseTo(expectedExpiry, within(1, ChronoUnit.MINUTES));
    }

    @Test
    void createTokenRejectsExpiryBeyondCertValidity() {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

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
    void validateTokenReturnsEnrollmentForValidToken() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

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
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

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
    void validateTokenReturnsEmptyForWrongClusterId() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

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
    void validateTokenReturnsEmptyForInvalidSignature() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

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
    void createTokenIncludesCttHeader() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        final CreateEnrollmentTokenRequest request = new CreateEnrollmentTokenRequest("test-fleet", Duration.ofDays(1));
        final EnrollmentTokenResponse response = enrollmentTokenService.createToken(request);

        // Decode header without verification
        final String[] parts = response.token().split("\\.");
        final String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);

        assertThat(headerJson).contains("\"ctt\":\"enrollment\"");
    }

    @Test
    void validateTokenReturnsEmptyForUnknownKid() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        // Create a valid token structure but with unknown kid
        // This is a token with header {"alg":"EdDSA","kid":"sha256:unknown"}
        final String fakeToken = "eyJhbGciOiJFZERTQSIsImtpZCI6InNoYTI1Njp1bmtub3duIn0.eyJpc3MiOiJodHRwczovL2dyYXlsb2cuZXhhbXBsZS5jb20vIn0.invalidsig";

        final Optional<OpAmpAuthContext.Enrollment> result = enrollmentTokenService.validateToken(
                fakeToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgentTokenReturnsIdentifiedForValidToken() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        // Create CA hierarchy
        final CertificateEntry enrollmentCa = enrollmentTokenService.getEnrollmentCa();

        // Generate agent key pair and CSR (use Ed25519 to match CA)
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final byte[] csrPem = createCsrPem("CN=test-agent", agentKeyPair, "Ed25519");

        // Sign CSR with enrollment CA - need to create a CertificateEntry from the signed X509Certificate
        final X509Certificate signedCert = certificateService.builder().signCsr(csrPem, enrollmentCa, "test-agent", Duration.ofDays(365));
        final String certFingerprint = PemUtils.computeFingerprint(signedCert);
        final String certPem = PemUtils.toPem(signedCert);

        // Save agent with cert
        final CollectorInstanceDTO collectorInstanceDTO = collectorInstanceService.enroll(
                "test-instance-uid",
                "test-fleet",
                certFingerprint,
                certPem,
                enrollmentCa.id(),
                Instant.now()
        );

        // Create agent JWT with x5t#S256 header (RFC 7515 format)
        final String agentToken = Jwts.builder()
                .header()
                    .add("typ", "agent+jwt")
                    .add("x5t#S256", PemUtils.fingerprintToX5t(certFingerprint))
                .and()
                .subject(collectorInstanceDTO.instanceUid())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(agentKeyPair.getPrivate())
                .compact();

        // Validate
        final Optional<OpAmpAuthContext.Identified> result = enrollmentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isPresent();
        assertThat(result.get().instanceUid()).isEqualTo("test-instance-uid");
        assertThat(result.get().transport()).isEqualTo(OpAmpAuthContext.Transport.HTTP);
    }

    @Test
    void validateAgentTokenReturnsEmptyForUnknownFingerprint() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        // Generate agent key pair (not enrolled)
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();

        // Create agent JWT with unknown fingerprint (using x5t#S256 format)
        // Use a valid base64url-encoded 32-byte value that won't match any agent
        final String unknownX5t = PemUtils.fingerprintToX5t("sha256:0000000000000000000000000000000000000000000000000000000000000000");
        final String agentToken = Jwts.builder()
                .header()
                    .add("typ", "agent+jwt")
                    .add("x5t#S256", unknownX5t)
                .and()
                .subject("unknown-agent")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(agentKeyPair.getPrivate())
                .compact();

        // Validate
        final Optional<OpAmpAuthContext.Identified> result = enrollmentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgentTokenReturnsEmptyForMissingX5tHeader() throws Exception {
        // Generate agent key pair
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();

        // Create agent JWT without x5t#S256 header
        final String agentToken = Jwts.builder()
                .header()
                    .add("typ", "agent+jwt")
                .and()
                .subject("agent")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(agentKeyPair.getPrivate())
                .compact();

        // Validate
        final Optional<OpAmpAuthContext.Identified> result = enrollmentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgentTokenReturnsEmptyForInvalidSignature() throws Exception {
        final AtomicReference<OpAmpCaConfig> storedConfig = new AtomicReference<>();
        when(clusterConfigService.get(OpAmpCaConfig.class)).thenAnswer(inv -> storedConfig.get());
        doAnswer(inv -> {
            storedConfig.set(inv.getArgument(0));
            return null;
        }).when(clusterConfigService).write(any(OpAmpCaConfig.class));

        // Create CA hierarchy
        final CertificateEntry enrollmentCa = enrollmentTokenService.getEnrollmentCa();

        // Generate agent key pair and CSR (use Ed25519 to match CA)
        final KeyPair agentKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final byte[] csrPem = createCsrPem("CN=test-agent", agentKeyPair, "Ed25519");

        // Sign CSR with enrollment CA
        final X509Certificate signedCert = certificateService.builder().signCsr(csrPem, enrollmentCa, "test-agent", Duration.ofDays(365));
        final String certFingerprint = PemUtils.computeFingerprint(signedCert);
        final String certPem = PemUtils.toPem(signedCert);

        // Save agent with cert
        final CollectorInstanceDTO collectorInstanceDTO = collectorInstanceService.enroll(
                "test-instance-uid-2",
                "test-fleet",
                certFingerprint,
                certPem,
                enrollmentCa.id(),
                Instant.now()
        );

        // Create JWT signed with a DIFFERENT key
        final KeyPair differentKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final String agentToken = Jwts.builder()
                .header()
                    .add("typ", "agent+jwt")
                    .add("x5t#S256", PemUtils.fingerprintToX5t(certFingerprint))
                .and()
                .subject(collectorInstanceDTO.instanceUid())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(differentKeyPair.getPrivate())
                .compact();

        // Validate - should fail because signature doesn't match certificate's public key
        final Optional<OpAmpAuthContext.Identified> result = enrollmentTokenService.validateAgentToken(
                agentToken, OpAmpAuthContext.Transport.HTTP);

        assertThat(result).isEmpty();
    }

    private byte[] createCsrPem(String subject, KeyPair keyPair, String algorithm) throws Exception {
        final X500Name x500Name = new X500Name(subject);
        final ContentSigner signer = new JcaContentSignerBuilder(algorithm)
                .build(keyPair.getPrivate());
        final PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(x500Name, keyPair.getPublic())
                .build(signer);

        final StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(csr);
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }
}
