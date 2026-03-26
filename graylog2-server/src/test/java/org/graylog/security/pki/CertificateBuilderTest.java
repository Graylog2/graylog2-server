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

import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.testing.TestClocks;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CertificateBuilder} certificate creation operations.
 */
class CertificateBuilderTest {

    private CertificateBuilder builder;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = TestClocks.fixedEpoch();
        builder = new CertificateBuilder(new EncryptedValueService("1234567890abcdef"), "Graylog", clock);
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
    void createRootCaCertificateHasSkiButNoAki() throws Exception {
        final var entry = builder.createRootCa("SKI Test", Algorithm.ED25519, Duration.ofDays(365));
        final var cert = PemUtils.parseCertificate(entry.certificate());

        assertThat(PemUtils.extractSubjectKeyIdentifier(cert)).isPresent();
        assertThat(PemUtils.extractAuthorityKeyIdentifier(cert)).isEmpty();
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
    void createIntermediateCaHasSkiAndAkiMatchingIssuerSki() throws Exception {
        final var rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));
        final var intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(365));

        final var rootCert = PemUtils.parseCertificate(rootCa.certificate());
        final var intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());

        assertThat(PemUtils.extractSubjectKeyIdentifier(intermediateCert)).isPresent();
        assertThat(PemUtils.extractAuthorityKeyIdentifier(intermediateCert))
                .isEqualTo(PemUtils.extractSubjectKeyIdentifier(rootCert));
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
    void createEndEntityCertHasSkiAndAkiMatchingIssuerSki() throws Exception {
        final var rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));
        final var intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(1825));
        final var endEntity = builder.createEndEntityCert(
                "Agent", intermediateCa, KeyUsage.digitalSignature, Duration.ofDays(365));

        final var intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());
        final var endEntityCert = PemUtils.parseCertificate(endEntity.certificate());

        assertThat(PemUtils.extractSubjectKeyIdentifier(endEntityCert)).isPresent();
        assertThat(PemUtils.extractAuthorityKeyIdentifier(endEntityCert))
                .isEqualTo(PemUtils.extractSubjectKeyIdentifier(intermediateCert));
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

        // Verify the chain
        final X509Certificate rootCert = PemUtils.parseCertificate(rootCa.certificate());
        final X509Certificate intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());

        // Root is self-signed
        rootCert.verify(rootCert.getPublicKey());
        // Intermediate is signed by root
        intermediateCert.verify(rootCert.getPublicKey());

        // Verify issuer chains
        assertThat(rootCa.issuerChain()).isEmpty();
        assertThat(intermediateCa.issuerChain()).containsExactly(rootCa.certificate());
    }

    @Test
    void createRootCaIncludesProductNameInSubject() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(365));
        final X509Certificate cert = PemUtils.parseCertificate(rootCa.certificate());

        final String subjectDn = cert.getSubjectX500Principal().getName();
        assertThat(subjectDn).contains("CN=Test CA");
        assertThat(subjectDn).contains("O=Graylog");
    }

    // End-entity certificate with EKU tests

    @Test
    void createEndEntityCertWithServerAuthEku() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "OTLP Server", rootCa, KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                KeyPurposeId.id_kp_serverAuth, Duration.ofDays(365)
        );

        final X509Certificate cert = PemUtils.parseCertificate(endEntityCert.certificate());

        // Verify Basic Constraints: CA:FALSE
        assertThat(cert.getBasicConstraints()).isEqualTo(-1);

        // Verify Extended Key Usage contains serverAuth (OID 1.3.6.1.5.5.7.3.1)
        final List<String> extendedKeyUsage = cert.getExtendedKeyUsage();
        assertThat(extendedKeyUsage).isNotNull();
        assertThat(extendedKeyUsage).contains("1.3.6.1.5.5.7.3.1");
    }

    @Test
    void createEndEntityCertWithClientAuthEku() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "Client Cert", rootCa, KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth, Duration.ofDays(365)
        );

        final X509Certificate cert = PemUtils.parseCertificate(endEntityCert.certificate());

        // Verify Extended Key Usage contains clientAuth (OID 1.3.6.1.5.5.7.3.2)
        final List<String> extendedKeyUsage = cert.getExtendedKeyUsage();
        assertThat(extendedKeyUsage).isNotNull();
        assertThat(extendedKeyUsage).contains("1.3.6.1.5.5.7.3.2");
    }

    @Test
    void createEndEntityCertWithoutEkuHasNoExtendedKeyUsage() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        // Use the original 4-arg method (no EKU)
        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "No EKU", rootCa, KeyUsage.digitalSignature, Duration.ofDays(365)
        );

        final X509Certificate cert = PemUtils.parseCertificate(endEntityCert.certificate());

        // Should NOT have Extended Key Usage
        assertThat(cert.getExtendedKeyUsage()).isNull();
    }

    // End-entity certificate with SAN tests

    @Test
    void createEndEntityCertWithDnsSans() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "OTLP Server", rootCa, KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                KeyPurposeId.id_kp_serverAuth, Duration.ofDays(365),
                List.of("2209F727-F7E1-4123-9386-94FE3B354A07")
        );

        final X509Certificate cert = PemUtils.parseCertificate(endEntityCert.certificate());

        // Verify the SAN extension is present with the expected dNSName
        assertThat(cert.getSubjectAlternativeNames()).isNotNull();
        assertThat(cert.getSubjectAlternativeNames()).anySatisfy(san -> {
            // GeneralName type 2 = dNSName
            assertThat(san.get(0)).isEqualTo(2);
            assertThat(san.get(1)).isEqualTo("2209F727-F7E1-4123-9386-94FE3B354A07");
        });
    }

    @Test
    void createEndEntityCertWithoutSansHasNoSanExtension() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(3650));

        // Use the 4-arg method (no EKU, no SAN)
        final CertificateEntry endEntityCert = builder.createEndEntityCert(
                "No SAN", rootCa, KeyUsage.digitalSignature, Duration.ofDays(365)
        );

        final X509Certificate cert = PemUtils.parseCertificate(endEntityCert.certificate());

        // Should NOT have Subject Alternative Names
        assertThat(cert.getSubjectAlternativeNames()).isNull();
    }

    // PKIX trust chain validation tests

    @Test
    void pkixAcceptsEndEntityCertSignedByIntermediateCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(1));
        final CertificateEntry endEntity = builder.createEndEntityCert("test-agent", intermediateCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, Duration.ofDays(1));

        final X509Certificate endEntityCert = PemUtils.parseCertificate(endEntity.certificate());
        final X509Certificate intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());

        final CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final var certPath = cf.generateCertPath(List.of(endEntityCert));
        final var params = new PKIXParameters(Set.of(new TrustAnchor(intermediateCert, null)));
        params.setRevocationEnabled(false);
        params.setDate(Date.from(clock.instant()));

        assertThatCode(() -> validator.validate(certPath, params)).doesNotThrowAnyException();
    }

    @Test
    void pkixValidatesFullChainFromEndEntityToRoot() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(1));
        final CertificateEntry endEntity = builder.createEndEntityCert("test-agent", intermediateCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, Duration.ofDays(1));

        final X509Certificate endEntityCert = PemUtils.parseCertificate(endEntity.certificate());
        final X509Certificate intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());
        final X509Certificate rootCert = PemUtils.parseCertificate(rootCa.certificate());

        final CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final var certPath = cf.generateCertPath(List.of(endEntityCert, intermediateCert));
        final var params = new PKIXParameters(Set.of(new TrustAnchor(rootCert, null)));
        params.setRevocationEnabled(false);
        params.setDate(Date.from(clock.instant()));

        assertThatCode(() -> validator.validate(certPath, params)).doesNotThrowAnyException();
    }

    @Test
    void pkixRejectsEndEntityCertNotSignedByTrustedCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(1));
        final X509Certificate intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());

        // Create a rogue CA and sign an end-entity cert with it
        final CertificateEntry rogueRootCa = builder.createRootCa("Rogue CA", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry rogueCert = builder.createEndEntityCert("rogue-agent", rogueRootCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, Duration.ofDays(1));

        final X509Certificate rogueEndEntityCert = PemUtils.parseCertificate(rogueCert.certificate());

        final CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final var certPath = cf.generateCertPath(List.of(rogueEndEntityCert));
        final var params = new PKIXParameters(Set.of(new TrustAnchor(intermediateCert, null)));
        params.setRevocationEnabled(false);

        assertThatThrownBy(() -> validator.validate(certPath, params))
                .isInstanceOf(CertPathValidatorException.class);
    }

    @Test
    void pkixAcceptsRsaEndEntityCertSignedByIntermediateCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root CA", Algorithm.RSA_4096, Duration.ofDays(1));
        final CertificateEntry intermediateCa = builder.createIntermediateCa("Intermediate CA", rootCa, Duration.ofDays(1));
        final CertificateEntry endEntity = builder.createEndEntityCert("test-agent", intermediateCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, Duration.ofDays(1));

        final X509Certificate endEntityCert = PemUtils.parseCertificate(endEntity.certificate());
        final X509Certificate intermediateCert = PemUtils.parseCertificate(intermediateCa.certificate());

        final CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final var certPath = cf.generateCertPath(List.of(endEntityCert));
        final var params = new PKIXParameters(Set.of(new TrustAnchor(intermediateCert, null)));
        params.setRevocationEnabled(false);
        params.setDate(Date.from(clock.instant()));

        assertThatCode(() -> validator.validate(certPath, params)).doesNotThrowAnyException();
    }
}
