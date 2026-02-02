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

import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CertificateBuilder} certificate creation operations.
 */
class CertificateBuilderTest {

    private CertificateBuilder builder;
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    void setUp() {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(encryptedValueService);
    }

    // Key pair generation tests

    @Test
    void generateKeyPairEd25519() throws Exception {
        final KeyPair keyPair = builder.generateKeyPair(Algorithm.ED25519);

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("Ed25519");
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("Ed25519");
    }

    @Test
    void generateKeyPairRsa4096() throws Exception {
        final KeyPair keyPair = builder.generateKeyPair(Algorithm.RSA_4096);

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void generateKeyPairProducesUniqueKeys() throws Exception {
        final KeyPair keyPair1 = builder.generateKeyPair(Algorithm.ED25519);
        final KeyPair keyPair2 = builder.generateKeyPair(Algorithm.ED25519);

        assertThat(keyPair1.getPublic().getEncoded()).isNotEqualTo(keyPair2.getPublic().getEncoded());
    }

    // Root CA creation tests

    @Test
    void createRootCaWithEd25519() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Test Root CA", Algorithm.ED25519, Duration.ofDays(365));

        assertThat(entry).isNotNull();
        assertThat(entry.id()).isNull(); // Not saved yet
        assertThat(entry.fingerprint()).startsWith("sha256:");
        assertThat(entry.certificate()).startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(entry.issuerChain()).isEmpty();
    }

    @Test
    void createRootCaWithRsa4096() throws Exception {
        final CertificateEntry entry = builder.createRootCa("RSA Root CA", Algorithm.RSA_4096, Duration.ofDays(365));

        assertThat(entry).isNotNull();
        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());
        assertThat(cert.getPublicKey().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void createRootCaCertificateIsSelfSigned() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Self Signed CA", Algorithm.ED25519, Duration.ofDays(365));

        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());
        assertThat(cert.getSubjectX500Principal()).isEqualTo(cert.getIssuerX500Principal());
    }

    @Test
    void createRootCaCertificateHasCABasicConstraints() throws Exception {
        final CertificateEntry entry = builder.createRootCa("CA Test", Algorithm.ED25519, Duration.ofDays(365));

        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());
        assertThat(cert.getBasicConstraints()).isGreaterThanOrEqualTo(0);
        assertThat(cert.getCriticalExtensionOIDs()).contains(Extension.basicConstraints.getId());
    }

    @Test
    void createRootCaCertificateHasKeyUsageExtension() throws Exception {
        final CertificateEntry entry = builder.createRootCa("KeyUsage Test", Algorithm.ED25519, Duration.ofDays(365));

        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());
        final boolean[] keyUsage = cert.getKeyUsage();
        assertThat(keyUsage).isNotNull();
        assertThat(keyUsage[5]).isTrue(); // keyCertSign
        assertThat(keyUsage[6]).isTrue(); // cRLSign
    }

    @Test
    void createRootCaCertificateValidityMatchesDuration() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Validity Test", Algorithm.ED25519, Duration.ofDays(365));

        final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());
        final Duration actualValidity = Duration.between(
                cert.getNotBefore().toInstant(),
                cert.getNotAfter().toInstant()
        );
        assertThat(actualValidity.toDays()).isEqualTo(365);
    }

    @Test
    void createRootCaStoresEncryptedPrivateKey() throws Exception {
        final CertificateEntry entry = builder.createRootCa("Encrypted Key Test", Algorithm.ED25519, Duration.ofDays(365));

        assertThat(entry.privateKey()).isNotNull();
        assertThat(entry.privateKey().isSet()).isTrue();
    }

    // Intermediate CA creation tests

    @Test
    void createIntermediateCaWithEd25519() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(365));

        assertThat(intermediateCa).isNotNull();
        assertThat(intermediateCa.id()).isNull(); // Not saved yet
        assertThat(intermediateCa.fingerprint()).startsWith("sha256:");
    }

    @Test
    void createIntermediateCaHasCorrectIssuer() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry intermediateCa = builder.createIntermediateCa("Test Intermediate CA", rootCa, Duration.ofDays(365));

        final X509Certificate cert = PemUtils.parseCertificate(intermediateCa.certificate());
        assertThat(cert.getSubjectX500Principal().getName()).contains("CN=Test Intermediate CA");
        assertThat(cert.getIssuerX500Principal().getName()).contains("CN=Test Root CA");
    }

    @Test
    void createIntermediateCaHasPathLen0BasicConstraints() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(365));

        final X509Certificate cert = PemUtils.parseCertificate(intermediateCa.certificate());
        assertThat(cert.getBasicConstraints()).isEqualTo(0); // pathLen:0
    }

    @Test
    void createIntermediateCaBuildsIssuerChain() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(365));

        assertThat(intermediateCa.issuerChain()).hasSize(1);
        assertThat(intermediateCa.issuerChain().get(0)).isEqualTo(rootCa.certificate());
    }

    @Test
    void createIntermediateCaIsSignedByIssuer() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(365));

        final X509Certificate rootCert = PemUtils.parseCertificate(rootCa.certificate());
        final X509Certificate intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());

        intermediateCert.verify(rootCert.getPublicKey());
    }

    @Test
    void createIntermediateCaInheritsAlgorithmFromIssuer() throws Exception {
        final CertificateEntry rsaRootCa = builder.createRootCa("RSA Root CA", Algorithm.RSA_4096, Duration.ofDays(3650));

        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rsaRootCa, Duration.ofDays(365));

        final X509Certificate cert = PemUtils.parseCertificate(intermediateCa.certificate());
        assertThat(cert.getPublicKey().getAlgorithm()).isEqualTo("RSA");
    }

    // End-entity certificate creation tests

    @Test
    void createEndEntityCertWithEd25519() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "Token Signing Key", rootCa, KeyUsage.digitalSignature, Duration.ofDays(365)
        );

        assertThat(endEntityCert).isNotNull();
        assertThat(endEntityCert.id()).isNull(); // Not saved yet
        assertThat(endEntityCert.fingerprint()).startsWith("sha256:");
    }

    @Test
    void createEndEntityCertHasCAFalseBasicConstraints() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "End Entity", rootCa, KeyUsage.digitalSignature, Duration.ofDays(365)
        );

        final X509Certificate cert = PemUtils.parseCertificate(endEntityCert.certificate());
        assertThat(cert.getBasicConstraints()).isEqualTo(-1); // Not a CA
    }

    @Test
    void createEndEntityCertHasConfigurableKeyUsage() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "End Entity", rootCa, KeyUsage.digitalSignature, Duration.ofDays(365)
        );

        final X509Certificate cert = PemUtils.parseCertificate(endEntityCert.certificate());
        final boolean[] keyUsage = cert.getKeyUsage();
        assertThat(keyUsage[0]).isTrue(); // digitalSignature
        assertThat(keyUsage[5]).isFalse(); // keyCertSign should NOT be set
    }

    @Test
    void createEndEntityCertBuildsIssuerChainFromIntermediateCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));
        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(1825));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "End Entity", intermediateCa, KeyUsage.digitalSignature, Duration.ofDays(365)
        );

        assertThat(endEntityCert.issuerChain()).hasSize(2);
        assertThat(endEntityCert.issuerChain().get(0)).isEqualTo(intermediateCa.certificate());
        assertThat(endEntityCert.issuerChain().get(1)).isEqualTo(rootCa.certificate());
    }

    @Test
    void createEndEntityCertIsSignedByIssuer() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "End Entity", rootCa, KeyUsage.digitalSignature, Duration.ofDays(365)
        );

        final X509Certificate rootCert = PemUtils.parseCertificate(rootCa.certificate());
        final X509Certificate endEntityX509 = PemUtils.parseCertificate(endEntityCert.certificate());

        endEntityX509.verify(rootCert.getPublicKey());
    }

    @Test
    void createFullCaHierarchy() throws Exception {
        // Test the full hierarchy: Root CA -> Intermediate CA -> End Entity
        final CertificateEntry rootCa = builder.createRootCa(
                "Graylog Root CA", Algorithm.ED25519, Duration.ofDays(30 * 365)
        );
        final CertificateEntry intermediateCa = builder.createIntermediateCa(
                "Graylog Enrollment CA", rootCa, Duration.ofDays(5 * 365)
        );
        final CertificateEntry tokenSigningCert = builder.createEndEntityCert(
                "Token Signing", intermediateCa, KeyUsage.digitalSignature, Duration.ofDays(2 * 365)
        );

        // Verify the chain
        final X509Certificate rootCert = PemUtils.parseCertificate(rootCa.certificate());
        final X509Certificate intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());
        final X509Certificate tokenCert = PemUtils.parseCertificate(tokenSigningCert.certificate());

        // Root is self-signed
        rootCert.verify(rootCert.getPublicKey());
        // Intermediate is signed by root
        intermediateCert.verify(rootCert.getPublicKey());
        // Token signing cert is signed by intermediate
        tokenCert.verify(intermediateCert.getPublicKey());

        // Verify issuer chains
        assertThat(rootCa.issuerChain()).isEmpty();
        assertThat(intermediateCa.issuerChain()).containsExactly(rootCa.certificate());
        assertThat(tokenSigningCert.issuerChain()).containsExactly(intermediateCa.certificate(), rootCa.certificate());
    }
}
