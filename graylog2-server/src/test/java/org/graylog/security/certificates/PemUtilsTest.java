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
package org.graylog.security.certificates;

import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PemUtils} PEM encoding/decoding utilities.
 */
class PemUtilsTest {

    private CertificateBuilder builder;
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    void setUp() {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(encryptedValueService);
    }

    // toPem(X509Certificate) tests

    @Test
    void toPemCertificateProducesValidPem() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(365));
        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());

        final String pem = PemUtils.toPem(cert);

        assertThat(pem).startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(pem).endsWith("-----END CERTIFICATE-----\n");
    }

    // toPem(PrivateKey) tests

    @Test
    void toPemPrivateKeyProducesValidPem() throws Exception {
        final KeyPair keyPair = builder.generateKeyPair(Algorithm.ED25519);

        final String pem = PemUtils.toPem(keyPair.getPrivate());

        assertThat(pem).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(pem).endsWith("-----END PRIVATE KEY-----\n");
    }

    // parseCertificate tests

    @Test
    void parseCertificateRoundTrips() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Round Trip Test", Algorithm.ED25519, Duration.ofDays(365));
        final X509Certificate originalCert = PemUtils.parseCertificate(entry.certificate());

        final String pem = PemUtils.toPem(originalCert);
        final X509Certificate parsedCert = PemUtils.parseCertificate(pem);

        assertThat(parsedCert.getSubjectX500Principal()).isEqualTo(originalCert.getSubjectX500Principal());
        assertThat(parsedCert.getSerialNumber()).isEqualTo(originalCert.getSerialNumber());
    }

    @Test
    void parseCertificateThrowsForInvalidPem() {
        assertThatThrownBy(() -> PemUtils.parseCertificate("not a certificate"))
                .isInstanceOf(Exception.class);
    }

    // parsePrivateKey tests

    @Test
    void parsePrivateKeyRoundTrips() throws Exception {
        final KeyPair keyPair = builder.generateKeyPair(Algorithm.ED25519);

        final String pem = PemUtils.toPem(keyPair.getPrivate());
        final PrivateKey parsedKey = PemUtils.parsePrivateKey(pem);

        assertThat(parsedKey.getAlgorithm()).isEqualTo("Ed25519");
        assertThat(parsedKey.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
    }

    @Test
    void parsePrivateKeyWorksWithRsa() throws Exception {
        final KeyPair keyPair = builder.generateKeyPair(Algorithm.RSA_4096);

        final String pem = PemUtils.toPem(keyPair.getPrivate());
        final PrivateKey parsedKey = PemUtils.parsePrivateKey(pem);

        assertThat(parsedKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void parsePrivateKeyThrowsForInvalidPem() {
        assertThatThrownBy(() -> PemUtils.parsePrivateKey("not a private key"))
                .isInstanceOf(Exception.class);
    }

    // computeFingerprint tests

    @Test
    void computeFingerprintReturnsCorrectFormat() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Fingerprint Test", Algorithm.ED25519, Duration.ofDays(365));
        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());

        final String fingerprint = PemUtils.computeFingerprint(cert);

        assertThat(fingerprint).startsWith("sha256:");
        assertThat(fingerprint).hasSize(71); // "sha256:" prefix + 64 hex characters
        assertThat(fingerprint.substring(7)).matches("[0-9a-f]{64}");
    }

    @Test
    void computeFingerprintIsDeterministic() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Deterministic Test", Algorithm.ED25519, Duration.ofDays(365));
        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());

        final String fingerprint1 = PemUtils.computeFingerprint(cert);
        final String fingerprint2 = PemUtils.computeFingerprint(cert);

        assertThat(fingerprint1).isEqualTo(fingerprint2);
    }

    @Test
    void computeFingerprintDiffersForDifferentCertificates() throws Exception {
        final CertificateEntry entry1 = builder.createRootCa("Test CA 1", Algorithm.ED25519, Duration.ofDays(365));
        final CertificateEntry entry2 = builder.createRootCa("Test CA 2", Algorithm.ED25519, Duration.ofDays(365));

        final X509Certificate cert1 = PemUtils.parseCertificate(entry1.certificate());
        final X509Certificate cert2 = PemUtils.parseCertificate(entry2.certificate());

        final String fingerprint1 = PemUtils.computeFingerprint(cert1);
        final String fingerprint2 = PemUtils.computeFingerprint(cert2);

        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }

    // detectAlgorithm tests

    @Test
    void detectAlgorithmReturnsEd25519ForEd25519Certificate() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Ed25519 CA", Algorithm.ED25519, Duration.ofDays(365));
        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());

        final Algorithm algorithm = PemUtils.detectAlgorithm(cert);

        assertThat(algorithm).isEqualTo(Algorithm.ED25519);
    }

    @Test
    void detectAlgorithmReturnsRsa4096ForRsaCertificate() throws Exception {
        final CertificateEntry entry = builder.createRootCa("RSA CA", Algorithm.RSA_4096, Duration.ofDays(365));
        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());

        final Algorithm algorithm = PemUtils.detectAlgorithm(cert);

        assertThat(algorithm).isEqualTo(Algorithm.RSA_4096);
    }

    // fingerprintToX5t tests

    @Test
    void fingerprintToX5tConvertsCorrectly() {
        // Known conversion: sha256 of empty string
        // SHA256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        // Base64url of those bytes = 47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU
        final String fingerprint = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        final String x5t = PemUtils.fingerprintToX5t(fingerprint);
        assertThat(x5t).isEqualTo("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU");
    }

    @Test
    void x5tToFingerprintConvertsCorrectly() {
        final String x5t = "47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU";
        final String fingerprint = PemUtils.x5tToFingerprint(x5t);
        assertThat(fingerprint).isEqualTo("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void fingerprintToX5tRoundTrip() throws Exception {
        // Create a certificate and verify round-trip
        final CertificateEntry cert = builder.createRootCa("Test", Algorithm.ED25519, Duration.ofDays(1));

        final String fingerprint = cert.fingerprint();
        final String x5t = PemUtils.fingerprintToX5t(fingerprint);
        final String roundTrip = PemUtils.x5tToFingerprint(x5t);

        assertThat(roundTrip).isEqualTo(fingerprint);
    }

    @Test
    void fingerprintToX5tRejectsInvalidFormat() {
        assertThatThrownBy(() -> PemUtils.fingerprintToX5t("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sha256:");
    }
}
