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
package org.graylog.security.certificates.jwks;

import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.security.certificates.Algorithm;
import org.graylog.security.certificates.CertificateBuilder;
import org.graylog.security.certificates.CertificateEntry;
import org.graylog.security.certificates.CertificateService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JwksService}.
 */
@ExtendWith(MockitoExtension.class)
class JwksServiceTest {

    @Mock
    private CertificateService certificateService;

    private JwksService jwksService;
    private CertificateBuilder builder;
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    void setUp() {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(encryptedValueService);
        jwksService = new JwksService(certificateService);
    }

    // getJwks tests

    @Test
    void getJwksReturnsEmptyResponseWhenNoCertificates() {
        when(certificateService.findAll()).thenReturn(List.of());

        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).isEmpty();
    }

    @Test
    void getJwksReturnsValidSigningCertificates() throws Exception {
        final CertificateEntry signingCert = createValidSigningCertificate();
        when(certificateService.findAll()).thenReturn(List.of(signingCert));

        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).hasSize(1);
        assertThat(response.keys().get(0).kid()).isEqualTo(signingCert.fingerprint());
        assertThat(response.keys().get(0).kty()).isEqualTo("OKP");
    }

    @Test
    void getJwksFiltersExpiredCertificates() throws Exception {
        final CertificateEntry expiredCert = createExpiredSigningCertificate();
        when(certificateService.findAll()).thenReturn(List.of(expiredCert));

        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).isEmpty();
    }

    @Test
    void getJwksFiltersNotYetValidCertificates() throws Exception {
        final CertificateEntry futureEntry = createFutureCertificate();
        when(certificateService.findAll()).thenReturn(List.of(futureEntry));

        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).isEmpty();
    }

    @Test
    void getJwksFiltersCertificatesWithoutDigitalSignatureUsage() throws Exception {
        // Root CA has keyCertSign usage, not digitalSignature
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(365));
        when(certificateService.findAll()).thenReturn(List.of(rootCa));

        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).isEmpty();
    }

    @Test
    void getJwksReturnsMultipleValidCertificates() throws Exception {
        final CertificateEntry signingCert1 = createValidSigningCertificate();
        final CertificateEntry signingCert2 = createValidSigningCertificate();
        when(certificateService.findAll()).thenReturn(List.of(signingCert1, signingCert2));

        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).hasSize(2);
    }

    // findByKid tests

    @Test
    void findByKidReturnsJwkForValidFingerprint() throws Exception {
        final CertificateEntry signingCert = createValidSigningCertificate();
        when(certificateService.findByFingerprint(signingCert.fingerprint())).thenReturn(Optional.of(signingCert));

        final Optional<Jwk> result = jwksService.findByKid(signingCert.fingerprint());

        assertThat(result).isPresent();
        assertThat(result.get().kid()).isEqualTo(signingCert.fingerprint());
    }

    @Test
    void findByKidReturnsEmptyForUnknownFingerprint() {
        when(certificateService.findByFingerprint(anyString())).thenReturn(Optional.empty());

        final Optional<Jwk> result = jwksService.findByKid("sha256:unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findByKidReturnsEmptyForExpiredCertificate() throws Exception {
        final CertificateEntry expiredCert = createExpiredSigningCertificate();
        when(certificateService.findByFingerprint(expiredCert.fingerprint())).thenReturn(Optional.of(expiredCert));

        final Optional<Jwk> result = jwksService.findByKid(expiredCert.fingerprint());

        assertThat(result).isEmpty();
    }

    @Test
    void findByKidReturnsEmptyForCertWithoutDigitalSignatureUsage() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(365));
        when(certificateService.findByFingerprint(rootCa.fingerprint())).thenReturn(Optional.of(rootCa));

        final Optional<Jwk> result = jwksService.findByKid(rootCa.fingerprint());

        assertThat(result).isEmpty();
    }

    // getCertExpiry tests

    @Test
    void getCertExpiryReturnsNotAfterForKnownCert() throws Exception {
        final CertificateEntry signingCert = createValidSigningCertificate();
        when(certificateService.findByFingerprint(signingCert.fingerprint())).thenReturn(Optional.of(signingCert));

        final Optional<Instant> result = jwksService.getCertExpiry(signingCert.fingerprint());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(signingCert.notAfter());
    }

    @Test
    void getCertExpiryReturnsEmptyForUnknownCert() {
        when(certificateService.findByFingerprint(anyString())).thenReturn(Optional.empty());

        final Optional<Instant> result = jwksService.getCertExpiry("sha256:unknown");

        assertThat(result).isEmpty();
    }

    // Helper methods

    private CertificateEntry createValidSigningCertificate() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test Root CA", Algorithm.ED25519, Duration.ofDays(365));
        return builder.createEndEntityCert("Token Signing", rootCa, KeyUsage.digitalSignature, Duration.ofDays(30));
    }

    private CertificateEntry createExpiredSigningCertificate() throws Exception {
        final CertificateEntry signingCert = createValidSigningCertificate();
        // Create a new entry with notAfter in the past
        return new CertificateEntry(
                signingCert.id(),
                signingCert.fingerprint(),
                signingCert.privateKey(),
                signingCert.certificate(),
                signingCert.issuerChain(),
                Instant.now().minus(Duration.ofDays(60)),
                Instant.now().minus(Duration.ofDays(1)), // Expired yesterday
                signingCert.createdAt()
        );
    }

    private CertificateEntry createFutureCertificate() throws Exception {
        final CertificateEntry signingCert = createValidSigningCertificate();
        // Create a new entry with notBefore in the future
        return new CertificateEntry(
                signingCert.id(),
                signingCert.fingerprint(),
                signingCert.privateKey(),
                signingCert.certificate(),
                signingCert.issuerChain(),
                Instant.now().plus(Duration.ofDays(1)), // Starts tomorrow
                Instant.now().plus(Duration.ofDays(30)),
                signingCert.createdAt()
        );
    }
}
