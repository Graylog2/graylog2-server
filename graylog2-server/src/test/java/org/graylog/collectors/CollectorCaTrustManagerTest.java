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
package org.graylog.collectors;

import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectorCaTrustManagerTest {
    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
    private final CertificateBuilder certBuilder = new CertificateBuilder(encryptedValueService, "Test", TestClocks.fixedEpoch());

    private CertificateEntry signingCertEntry;
    private CollectorCaTrustManager trustManager;
    private CollectorCaCache caCache;

    @BeforeEach
    void setUp() throws Exception {
        final var caCertEntry = certBuilder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(365));

        signingCertEntry = certBuilder.createIntermediateCa("Test Signing", caCertEntry, Duration.ofDays(365));

        caCache = mock(CollectorCaCache.class);
        // Look up the signing cert by its SKI (matched via the client cert's AKI)
        final var signingCert = PemUtils.parseCertificate(signingCertEntry.certificate());
        final var signingCertSki = PemUtils.extractSubjectKeyIdentifier(signingCert).orElseThrow();
        when(caCache.getBySubjectKeyIdentifier(signingCertSki)).thenReturn(Optional.of(cacheEntry(signingCertEntry)));
        when(caCache.getSigning()).thenReturn(cacheEntry(signingCertEntry));
        when(caCache.getCa()).thenReturn(cacheEntry(caCertEntry));

        trustManager = new CollectorCaTrustManager(caCache, TestClocks.fixedEpoch());
    }

    @Test
    void checkClientTrusted_acceptsCertSignedBySigningCert() throws Exception {
        final var clientCertEntry = certBuilder.createEndEntityCert(
                "test-agent", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var clientCert = PemUtils.parseCertificate(clientCertEntry.certificate());

        trustManager.checkClientTrusted(new X509Certificate[]{clientCert}, "Ed25519");
    }

    // The JDK TLS implementation reports "UNKNOWN" as the authType for Ed25519 client certificates
    // because it has no well-known auth type label for Ed25519 in its cipher suite mapping.
    @Test
    void checkClientTrusted_acceptsUnknownAuthType() throws Exception {
        final var clientCertEntry = certBuilder.createEndEntityCert(
                "test-agent", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var clientCert = PemUtils.parseCertificate(clientCertEntry.certificate());

        trustManager.checkClientTrusted(new X509Certificate[]{clientCert}, "UNKNOWN");
    }

    @Test
    void checkClientTrusted_rejectsNullCerts() {
        assertThatThrownBy(() -> trustManager.checkClientTrusted(null, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessage("No client certificates provided");
    }

    @Test
    void checkClientTrusted_rejectsEmptyCerts() {
        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[0], "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessage("No client certificates provided");
    }

    @Test
    void checkClientTrusted_rejectsCertSignedByUnknownCa() throws Exception {
        final var otherCa = certBuilder.createRootCa("Other CA", Algorithm.ED25519, Duration.ofDays(365));
        final var otherSigning = certBuilder.createIntermediateCa("Other Signing", otherCa, Duration.ofDays(365));
        final var untrustedCertEntry = certBuilder.createEndEntityCert(
                "rogue-agent", otherSigning, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var untrustedCert = PemUtils.parseCertificate(untrustedCertEntry.certificate());

        // The AKI of the untrusted cert won't match any known issuer SKI
        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{untrustedCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("No known issuer for Authority Key Identifier");
    }

    @Test
    void checkClientTrusted_rejectsCertWithKnownIssuerButBadSignature() throws Exception {
        // Create a cert signed by a different CA but forge the AKI to match our signing cert
        final var otherCa = certBuilder.createRootCa("Other CA", Algorithm.ED25519, Duration.ofDays(365));
        final var otherSigning = certBuilder.createIntermediateCa("Other Signing", otherCa, Duration.ofDays(365));
        final var forgedCertEntry = certBuilder.createEndEntityCert(
                "forged-agent", otherSigning, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var forgedCert = PemUtils.parseCertificate(forgedCertEntry.certificate());

        // Make the AKI lookup return our signing cert, but the signature won't match
        final var forgedAki = PemUtils.extractAuthorityKeyIdentifier(forgedCert).orElseThrow();
        when(caCache.getBySubjectKeyIdentifier(forgedAki)).thenReturn(Optional.of(cacheEntry(signingCertEntry)));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{forgedCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("Client certificate verification failed");
    }

    @Test
    void checkClientTrusted_rejectsExpiredCert() throws Exception {
        // Create a cert that was valid for 1 day starting at epoch
        final var shortLivedCertEntry = certBuilder.createEndEntityCert(
                "expired-agent", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(1));
        final var expiredCert = PemUtils.parseCertificate(shortLivedCertEntry.certificate());

        // Use a clock far in the future so the cert is expired
        final var futureClock = Clock.fixed(Instant.EPOCH.plus(Duration.ofDays(365)), ZoneOffset.UTC);
        final var futureTrustManager = new CollectorCaTrustManager(caCache, futureClock);

        assertThatThrownBy(() -> futureTrustManager.checkClientTrusted(new X509Certificate[]{expiredCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void checkClientTrusted_rejectsCertWithoutClientAuthEku() throws Exception {
        final var serverCertEntry = certBuilder.createEndEntityCert(
                "server-only", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_serverAuth, Duration.ofDays(30));
        final var serverCert = PemUtils.parseCertificate(serverCertEntry.certificate());

        final var serverCertAki = PemUtils.extractAuthorityKeyIdentifier(serverCert).orElseThrow();
        when(caCache.getBySubjectKeyIdentifier(serverCertAki)).thenReturn(Optional.of(cacheEntry(signingCertEntry)));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{serverCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("clientAuth extended key usage");
    }

    @Test
    void checkClientTrusted_rejectsCaCertUsedAsClientCert() throws Exception {
        // An intermediate CA cert has basicConstraints isCA=true and should be rejected even
        // when signature verification passes. The signing cert's AKI points to the root CA,
        // so we mock that lookup to return the root CA entry.
        final var signingCert = PemUtils.parseCertificate(signingCertEntry.certificate());
        final var signingCertAki = PemUtils.extractAuthorityKeyIdentifier(signingCert).orElseThrow();
        final var caCacheEntry = caCache.getCa();
        when(caCache.getBySubjectKeyIdentifier(signingCertAki)).thenReturn(Optional.of(caCacheEntry));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{signingCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("end-entity certificate, not a CA");
    }

    @Test
    void checkClientTrusted_rejectsIssuerWithoutCaBasicConstraints() throws Exception {
        // Create an end-entity cert and use it as the "issuer" in the SKI lookup.
        // Even if the signature would verify, the issuer must be a CA.
        final var endEntityEntry = certBuilder.createEndEntityCert(
                "not-a-ca", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_serverAuth, Duration.ofDays(30));

        // Create a client cert signed by the real signing cert
        final var clientCertEntry = certBuilder.createEndEntityCert(
                "test-agent", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var clientCert = PemUtils.parseCertificate(clientCertEntry.certificate());

        // Make the AKI lookup return the end-entity cert instead of the real signing cert
        final var clientAki = PemUtils.extractAuthorityKeyIdentifier(clientCert).orElseThrow();
        when(caCache.getBySubjectKeyIdentifier(clientAki)).thenReturn(Optional.of(cacheEntry(endEntityEntry)));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{clientCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("Issuer certificate is not a CA");
    }

    @Test
    void checkClientTrusted_rejectsIssuerWithoutKeyCertSign() throws Exception {
        // Create a valid client cert signed by the real signing cert
        final var clientCertEntry = certBuilder.createEndEntityCert(
                "test-agent", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var clientCert = PemUtils.parseCertificate(clientCertEntry.certificate());

        // Create a mock issuer cert that is a CA but lacks keyCertSign
        final var fakeIssuer = mock(X509Certificate.class);
        when(fakeIssuer.getBasicConstraints()).thenReturn(0); // CA:TRUE
        when(fakeIssuer.getKeyUsage()).thenReturn(new boolean[]{true, false, false, false, false, false, false, false, false});

        final var clientAki = PemUtils.extractAuthorityKeyIdentifier(clientCert).orElseThrow();
        final var fakeEntry = new CollectorCaCache.CacheEntry(null, fakeIssuer, "fake");
        when(caCache.getBySubjectKeyIdentifier(clientAki)).thenReturn(Optional.of(fakeEntry));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{clientCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("keyCertSign");
    }

    @Test
    void checkClientTrusted_rejectsIssuerNotSignedByRootCa() throws Exception {
        // Create a separate CA hierarchy not rooted in our trust anchor
        final var rogueRoot = certBuilder.createRootCa("Rogue Root", Algorithm.ED25519, Duration.ofDays(365));
        final var rogueSigning = certBuilder.createIntermediateCa("Rogue Signing", rogueRoot, Duration.ofDays(365));
        final var clientCertEntry = certBuilder.createEndEntityCert(
                "rogue-agent", rogueSigning, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var clientCert = PemUtils.parseCertificate(clientCertEntry.certificate());

        // Mock SKI lookup to return the rogue signing cert (valid CA, but wrong root)
        final var clientAki = PemUtils.extractAuthorityKeyIdentifier(clientCert).orElseThrow();
        when(caCache.getBySubjectKeyIdentifier(clientAki)).thenReturn(Optional.of(cacheEntry(rogueSigning)));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{clientCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("not signed by the collectors root CA");
    }

    @Test
    void checkClientTrusted_rejectsExpiredIssuer() throws Exception {
        // Create a valid client cert signed by the real signing cert
        final var clientCertEntry = certBuilder.createEndEntityCert(
                "test-agent", signingCertEntry, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(30));
        final var clientCert = PemUtils.parseCertificate(clientCertEntry.certificate());

        // Mock an expired issuer: a CA with keyCertSign, but checkValidity throws
        final var expiredIssuer = mock(X509Certificate.class);
        when(expiredIssuer.getBasicConstraints()).thenReturn(0);
        when(expiredIssuer.getKeyUsage()).thenReturn(new boolean[]{false, false, false, false, false, true, false, false, false});
        doThrow(new java.security.cert.CertificateExpiredException("expired")).when(expiredIssuer).checkValidity(any());

        final var clientAki = PemUtils.extractAuthorityKeyIdentifier(clientCert).orElseThrow();
        final var fakeEntry = new CollectorCaCache.CacheEntry(null, expiredIssuer, "expired-issuer");
        when(caCache.getBySubjectKeyIdentifier(clientAki)).thenReturn(Optional.of(fakeEntry));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{clientCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void checkClientTrusted_rejectsSelfSignedCert() throws Exception {
        // A self-signed root CA cert has no AKI, so it should be rejected
        final var selfSignedCa = certBuilder.createRootCa("Self Signed", Algorithm.ED25519, Duration.ofDays(365));
        final var selfSignedCert = PemUtils.parseCertificate(selfSignedCa.certificate());

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{selfSignedCert}, "Ed25519"))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("no Authority Key Identifier");
    }

    @Test
    void checkServerTrusted_throwsUnsupportedOperationException() {
        assertThatThrownBy(() -> trustManager.checkServerTrusted(new X509Certificate[0], "Ed25519"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getAcceptedIssuers_returnsEmptyArray() {
        assertThat(trustManager.getAcceptedIssuers()).isEmpty();
    }

    private CollectorCaCache.CacheEntry cacheEntry(CertificateEntry entry) throws Exception {
        final var cert = PemUtils.parseCertificate(entry.certificate());
        final var privateKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(entry.privateKey()));
        return new CollectorCaCache.CacheEntry(privateKey, cert, entry.fingerprint());
    }
}
