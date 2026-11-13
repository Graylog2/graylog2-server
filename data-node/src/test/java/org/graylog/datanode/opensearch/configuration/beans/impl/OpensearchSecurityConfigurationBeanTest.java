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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import org.graylog.datanode.Configuration;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.variants.OpensearchCertificates;
import org.graylog.datanode.configuration.variants.OpensearchCertificatesProvider;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.security.certutil.CaTruststore;
import org.graylog.security.certutil.CaTruststoreException;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.ca.exceptions.CaDecodeException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.security.JwtSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog.security.certutil.CertConstants.PKCS12;

class OpensearchSecurityConfigurationBeanTest {

    private static final CertificateGenerator CERTIFICATE_GENERATOR = new CertificateGenerator(1024);
    private static final Duration VALIDITY = Duration.ofDays(1);
    private static final JwtSecret JWT_SECRET = new JwtSecret("thisisverysecretpasswordthisisverysecretpasswordthisisverysecret");

    @TempDir
    private Path tempDir;

    private Configuration localConfiguration;
    private DatanodeConfiguration datanodeConfiguration;
    private KeyPair graylogCa;

    @BeforeEach
    void setUp() throws Exception {
        localConfiguration = DatanodeTestUtils.datanodeConfiguration(Map.of(), tempDir);
        datanodeConfiguration = DatanodeTestUtils.mockDatanodeConfiguration(tempDir);
        graylogCa = generateCa("Graylog CA");
    }

    @Test
    void addsGraylogCaForUserProvidedCertificates() throws Exception {
        // user provided certificates, issued by a CA unrelated to the Graylog CA
        final KeyPair customCa = generateCa("Custom CA");
        final KeyPair nodeCert = generateSigned("datanode", customCa);

        final DatanodeConfigurationPart part = buildConfigurationPart(certificates(nodeCert, customCa), caTruststore(graylogCa));

        assertThat(trusts(part.trustStore(), customCa.certificate())).isTrue();
        assertThat(trusts(part.trustStore(), graylogCa.certificate())).isTrue();
    }

    @Test
    void doesNotDuplicateGraylogCaAlreadyInCertificateChain() throws Exception {
        // Graylog managed certificates, the CA is already part of the chain
        final KeyPair nodeCert = generateSigned("datanode", graylogCa);

        final DatanodeConfigurationPart part = buildConfigurationPart(certificates(nodeCert, graylogCa), caTruststore(graylogCa));

        assertThat(countOccurrences(part.trustStore(), graylogCa.certificate())).isEqualTo(1);
    }

    @Test
    void worksWithoutGraylogCa() throws Exception {
        final KeyPair customCa = generateCa("Custom CA");
        final KeyPair nodeCert = generateSigned("datanode", customCa);

        final DatanodeConfigurationPart part = buildConfigurationPart(certificates(nodeCert, customCa), Optional::empty);

        assertThat(trusts(part.trustStore(), customCa.certificate())).isTrue();
        assertThat(trusts(part.trustStore(), graylogCa.certificate())).isFalse();
    }

    @Test
    void failureToLoadGraylogCaDoesNotBreakConfiguration() throws Exception {
        final KeyPair customCa = generateCa("Custom CA");
        final KeyPair nodeCert = generateSigned("datanode", customCa);

        final List<RuntimeException> failures = List.of(
                new CaTruststoreException(new KeyStoreStorageException("Could not read keystore")), // CA from filesystem
                new CaDecodeException("Failed to decode CA keystore", new IllegalArgumentException()), // CA from mongodb
                new RuntimeException("Failed to load keystore from Mongo collection") // CA from mongodb
        );

        for (RuntimeException failure : failures) {
            final CaTruststore failing = () -> {
                throw failure;
            };

            final DatanodeConfigurationPart part = buildConfigurationPart(certificates(nodeCert, customCa), failing);

            assertThat(part.securityConfigured()).as(failure.getClass().getSimpleName()).isTrue();
            assertThat(trusts(part.trustStore(), customCa.certificate())).as(failure.getClass().getSimpleName()).isTrue();
        }
    }

    @Test
    void addsOnlyChainOfConfiguredAlias() throws Exception {
        final KeyPair customCa = generateCa("Custom CA");
        final KeyPair nodeCert = generateSigned("datanode", customCa);
        final KeyPair unrelatedCa = generateCa("Unrelated CA");
        final KeyPair unrelatedCert = generateSigned("unrelated", unrelatedCa);

        final char[] password = "password".toCharArray();
        final KeyStore keystore = KeyStore.getInstance(PKCS12);
        keystore.load(null, null);
        keystore.setKeyEntry("unrelated", unrelatedCert.privateKey(), password, new Certificate[]{unrelatedCert.certificate(), unrelatedCa.certificate()});
        keystore.setKeyEntry("datanode", nodeCert.privateKey(), password, new Certificate[]{nodeCert.certificate(), customCa.certificate()});
        final Optional<OpensearchCertificates> certificates = Optional.of(new OpensearchCertificates(password, () -> keystore, "datanode", () -> keystore, "datanode"));

        final DatanodeConfigurationPart part = buildConfigurationPart(certificates, caTruststore(graylogCa));

        assertThat(trusts(part.trustStore(), customCa.certificate())).isTrue();
        assertThat(trusts(part.trustStore(), graylogCa.certificate())).isTrue();
        assertThat(trusts(part.trustStore(), unrelatedCa.certificate())).isFalse();
        assertThat(trusts(part.trustStore(), unrelatedCert.certificate())).isFalse();
    }

    @Test
    void doesNotAddGraylogCaWithoutCertificates() throws Exception {
        final DatanodeConfigurationPart part = buildConfigurationPart(Optional.empty(), caTruststore(graylogCa));

        assertThat(trusts(part.trustStore(), graylogCa.certificate())).isFalse();
    }

    private DatanodeConfigurationPart buildConfigurationPart(Optional<OpensearchCertificates> certificates, CaTruststore caTruststore) {
        final OpensearchCertificatesProvider provider = new OpensearchCertificatesProvider() {
            @Override
            public boolean isConfigured(Configuration localConfiguration) {
                return certificates.isPresent();
            }

            @Override
            public OpensearchCertificates build() {
                return certificates.orElseThrow();
            }
        };
        final OpensearchSecurityConfigurationBean bean = new OpensearchSecurityConfigurationBean(Set.of(provider), localConfiguration, JWT_SECRET, caTruststore);
        return bean.buildConfigurationPart(new OpensearchConfigurationParams(datanodeConfiguration, tempDir));
    }

    private static Optional<OpensearchCertificates> certificates(KeyPair nodeCert, KeyPair issuer) throws Exception {
        final char[] password = "password".toCharArray();
        final KeyStore keystore = KeyStore.getInstance(PKCS12);
        keystore.load(null, null);
        keystore.setKeyEntry("datanode", nodeCert.privateKey(), password, new Certificate[]{nodeCert.certificate(), issuer.certificate()});
        return Optional.of(new OpensearchCertificates(password, () -> keystore, "datanode", () -> keystore, "datanode"));
    }

    private static CaTruststore caTruststore(KeyPair ca) throws Exception {
        final KeyStore truststore = KeyStore.getInstance(PKCS12);
        truststore.load(null, null);
        truststore.setCertificateEntry(CA_KEY_ALIAS, ca.certificate());
        return () -> Optional.of(truststore);
    }

    private static KeyPair generateCa(String cn) throws Exception {
        return CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned(cn).isCA(true).validity(VALIDITY));
    }

    private static KeyPair generateSigned(String cn, KeyPair issuer) throws Exception {
        return CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.signed(cn, issuer).isCA(false).validity(VALIDITY));
    }

    private static boolean trusts(KeyStore truststore, X509Certificate certificate) throws Exception {
        return truststore.getCertificateAlias(certificate) != null;
    }

    private static long countOccurrences(KeyStore truststore, X509Certificate certificate) throws Exception {
        return Collections.list(truststore.aliases()).stream()
                .filter(alias -> {
                    try {
                        return certificate.equals(truststore.getCertificate(alias));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .count();
    }
}
