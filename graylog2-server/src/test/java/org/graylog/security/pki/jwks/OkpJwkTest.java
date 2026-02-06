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
package org.graylog.security.pki.jwks;

import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.security.interfaces.EdECPublicKey;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OkpJwk}.
 */
class OkpJwkTest {

    private CertificateBuilder builder;

    @BeforeEach
    void setUp() {
        final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(encryptedValueService);
    }

    @Test
    void fromPublicKeyReturnsOkpKeyType() throws Exception {
        final EdECPublicKey publicKey = createEd25519PublicKey();

        final OkpJwk jwk = OkpJwk.fromPublicKey("test-kid", publicKey);

        assertThat(jwk.kty()).isEqualTo("OKP");
    }

    @Test
    void fromPublicKeyExtractsCorrectCurve() throws Exception {
        final EdECPublicKey publicKey = createEd25519PublicKey();

        final OkpJwk jwk = OkpJwk.fromPublicKey("test-kid", publicKey);

        assertThat(jwk.crv()).isEqualTo("Ed25519");
    }

    @Test
    void fromPublicKeyExtractsPublicKeyAsBase64Url() throws Exception {
        final EdECPublicKey publicKey = createEd25519PublicKey();

        final OkpJwk jwk = OkpJwk.fromPublicKey("test-kid", publicKey);

        // The x value should be a valid base64url string without padding
        assertThat(jwk.x()).isNotNull();
        assertThat(jwk.x()).doesNotContain("="); // No padding
        assertThat(jwk.x()).doesNotContain("+"); // Not standard base64
        assertThat(jwk.x()).doesNotContain("/"); // Not standard base64

        // Should be 32 bytes (256 bits) for Ed25519, encoded as ~43 chars
        final byte[] decoded = Base64.getUrlDecoder().decode(jwk.x());
        assertThat(decoded).hasSize(32);
    }

    @Test
    void fromPublicKeySetsKeyId() throws Exception {
        final EdECPublicKey publicKey = createEd25519PublicKey();

        final OkpJwk jwk = OkpJwk.fromPublicKey("sha256:abc123", publicKey);

        assertThat(jwk.kid()).isEqualTo("sha256:abc123");
    }

    @Test
    void fromPublicKeySetsUsageToSig() throws Exception {
        final EdECPublicKey publicKey = createEd25519PublicKey();

        final OkpJwk jwk = OkpJwk.fromPublicKey("test-kid", publicKey);

        assertThat(jwk.use()).isEqualTo("sig");
    }

    @Test
    void fromPublicKeyProducesConsistentOutput() throws Exception {
        final EdECPublicKey publicKey = createEd25519PublicKey();

        final OkpJwk jwk1 = OkpJwk.fromPublicKey("test-kid", publicKey);
        final OkpJwk jwk2 = OkpJwk.fromPublicKey("test-kid", publicKey);

        assertThat(jwk1).isEqualTo(jwk2);
    }

    private EdECPublicKey createEd25519PublicKey() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test Root CA", Algorithm.ED25519, Duration.ofDays(365));
        final CertificateEntry signingCert = builder.createEndEntityCert(
                "Token Signing", rootCa, KeyUsage.digitalSignature, Duration.ofDays(30)
        );
        final X509Certificate cert = PemUtils.parseCertificate(signingCert.certificate());
        return (EdECPublicKey) cert.getPublicKey();
    }
}
