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
package org.graylog.security.pki;

import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.testing.TestClocks;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
        builder = new CertificateBuilder(encryptedValueService, "Graylog", TestClocks.fixedEpoch());
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
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);

        final String pem = PemUtils.toPem(keyPair.getPrivate());

        assertThat(pem).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(pem).endsWith("-----END PRIVATE KEY-----\n");
    }

    // toPem(PublicKey) tests

    @Test
    void toPemPublicKeyProducesValidPem() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);

        final String pem = PemUtils.toPem(keyPair.getPublic());

        assertThat(pem).startsWith("-----BEGIN PUBLIC KEY-----");
        assertThat(pem).endsWith("-----END PUBLIC KEY-----\n");
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
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);

        final String pem = PemUtils.toPem(keyPair.getPrivate());
        final PrivateKey parsedKey = PemUtils.parsePrivateKey(pem);

        assertThat(parsedKey.getAlgorithm()).isIn("Ed25519", "EdDSA");
        // Verify functional equivalence: sign with the parsed key and verify with the original public key.
        // We cannot compare getEncoded() directly because BC and JDK use different PKCS#8 variants
        // (BC includes the public key, JDK does not).
        final var sig = java.security.Signature.getInstance("Ed25519");
        sig.initSign(parsedKey);
        sig.update("test".getBytes(StandardCharsets.UTF_8));

        final byte[] signature = sig.sign();

        final var vrf = java.security.Signature.getInstance("Ed25519");
        vrf.initVerify(keyPair.getPublic());
        vrf.update("test".getBytes(StandardCharsets.UTF_8));
        assertThat(vrf.verify(signature)).isTrue();
    }

    @Test
    void parsePrivateKeyWorksWithRsa() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.RSA_4096);

        final String pem = PemUtils.toPem(keyPair.getPrivate());
        final PrivateKey parsedKey = PemUtils.parsePrivateKey(pem);

        assertThat(parsedKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void parsePrivateKeyThrowsForInvalidPem() {
        assertThatThrownBy(() -> PemUtils.parsePrivateKey("not a private key"))
                .isInstanceOf(IOException.class);
    }

    // parsePublicKey tests

    @Test
    void parsePublicKeyRoundTrips() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);

        final String pem = PemUtils.toPem(keyPair.getPublic());
        final PublicKey parsedKey = PemUtils.parsePublicKey(pem);

        assertThat(parsedKey.getAlgorithm()).isIn("Ed25519", "EdDSA");
        // Verify functional equivalence: sign with the private key and verify with the parsed public key.
        // We cannot compare getEncoded() directly because BC and JDK use different PKCS#8 variants
        // (BC includes the public key, JDK does not).
        final var sig = java.security.Signature.getInstance("Ed25519");
        sig.initSign(keyPair.getPrivate());
        sig.update("test".getBytes(StandardCharsets.UTF_8));

        final byte[] signature = sig.sign();

        final var vrf = java.security.Signature.getInstance("Ed25519");
        vrf.initVerify(parsedKey);
        vrf.update("test".getBytes(StandardCharsets.UTF_8));
        assertThat(vrf.verify(signature)).isTrue();
    }

    @Test
    void parsePublicKeyWorksWithRsa() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.RSA_4096);

        final String pem = PemUtils.toPem(keyPair.getPublic());
        final PublicKey parsedKey = PemUtils.parsePublicKey(pem);

        assertThat(parsedKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void parsePublicKeyThrowsForInvalidPem() {
        assertThatThrownBy(() -> PemUtils.parsePublicKey("not a public key"))
                .isInstanceOf(IOException.class);
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

    // extractSubjectKeyIdentifier tests

    @Test
    void extractSubjectKeyIdentifierReturnsHexString() throws Exception {
        final var cert = PemUtils.parseCertificate(
                builder.createRootCa("Test", Algorithm.ED25519, Duration.ofDays(365)).certificate());

        final var ski = PemUtils.extractSubjectKeyIdentifier(cert);

        assertThat(ski).isPresent();
        assertThat(ski.get()).matches("[0-9a-f]{40}"); // SHA-1 = 20 bytes = 40 hex chars
    }

    @Test
    void extractSubjectKeyIdentifierDiffersForDifferentKeys() throws Exception {
        final var cert1 = PemUtils.parseCertificate(
                builder.createRootCa("CA 1", Algorithm.ED25519, Duration.ofDays(365)).certificate());
        final var cert2 = PemUtils.parseCertificate(
                builder.createRootCa("CA 2", Algorithm.ED25519, Duration.ofDays(365)).certificate());

        assertThat(PemUtils.extractSubjectKeyIdentifier(cert1))
                .isNotEqualTo(PemUtils.extractSubjectKeyIdentifier(cert2));
    }

    // extractAuthorityKeyIdentifier tests

    @Test
    void extractAuthorityKeyIdentifierReturnsEmptyWhenAbsent() throws Exception {
        // Root CA has no AKI
        final var cert = PemUtils.parseCertificate(
                builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(365)).certificate());

        assertThat(PemUtils.extractAuthorityKeyIdentifier(cert)).isEmpty();
    }

    @Test
    void extractAuthorityKeyIdentifierMatchesIssuerSki() throws Exception {
        final var issuer = builder.createRootCa("Issuer", Algorithm.ED25519, Duration.ofDays(365));
        final var child = builder.createIntermediateCa("Child", issuer, Duration.ofDays(365));

        final var issuerSki = PemUtils.extractSubjectKeyIdentifier(PemUtils.parseCertificate(issuer.certificate()));
        final var childAki = PemUtils.extractAuthorityKeyIdentifier(PemUtils.parseCertificate(child.certificate()));

        assertThat(childAki).isEqualTo(issuerSki);
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

    // extractCn tests

    @Test
    void extractCnReturnsCommonName() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry agentCert = builder.createEndEntityCert(
                "my-agent-uid", rootCa,
                KeyUsage.digitalSignature,
                Duration.ofDays(1));
        final X509Certificate x509 = PemUtils.parseCertificate(agentCert.certificate());

        final String cn = PemUtils.extractCn(x509);

        assertThat(cn).isEqualTo("my-agent-uid");
    }

    @Test
    void extractCnWorksForRootCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(1));
        final X509Certificate x509 = PemUtils.parseCertificate(rootCa.certificate());

        final String cn = PemUtils.extractCn(x509);

        assertThat(cn).isEqualTo("Test CA");
    }
}
