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
package org.graylog.datanode.configuration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.assertj.core.api.Assertions;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

class DatanodeKeystoreTest {

    private static final CertificateGenerator CERTIFICATE_GENERATOR = new CertificateGenerator(1024);

    private EventBus eventBus;
    private final List<Object> receivedEvents = new LinkedList<>();

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        eventBus.register(this);
    }

    @AfterEach
    void tearDown() {
        eventBus.unregister(this);
    }

    @Subscribe
    public void subscribe(DatanodeCertificateChangedEvent event) {
        // remember received events so we can verify them later
        receivedEvents.add(event);
    }

    @Subscribe
    public void subscribe(DatanodeCertificateRenewedEvent event) {
        // remember received events so we can verify them later
        receivedEvents.add(event);
    }

    @Test
    void testCreateRead(@TempDir Path tempDir) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(DatanodeTestUtils.tempDirectories(tempDir), "foobar", this.eventBus);
        Assertions.assertThat(datanodeKeystore.exists()).isFalse();

        final KeyPair keyPair = DatanodeTestUtils.generateKeyPair(Duration.ofDays(30));

        datanodeKeystore.create(keyPair);
        Assertions.assertThat(datanodeKeystore.exists()).isTrue();

        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isFalse();
        final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest("my-hostname", List.of("second-hostname"));
        Assertions.assertThat(csr.getSubject().toString()).isEqualTo("CN=my-hostname");

        final CsrSigner signer = new CsrSigner();
        final KeyPair ca = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("Graylog CA").isCA(true).validity(Duration.ofDays(365)));
        final X509Certificate datanodeCert = signer.sign(ca.privateKey(), ca.certificate(), csr, 30);
        final CertificateChain certChain = new CertificateChain(datanodeCert, List.of(ca.certificate()));

        datanodeKeystore.replaceCertificatesInKeystore(certChain);
        Assertions.assertThat(datanodeKeystore.getSubjectAlternativeNames())
                .hasSizeGreaterThanOrEqualTo(2)
                .contains("my-hostname", "second-hostname");

        Assertions.assertThat(this.receivedEvents).hasSize(1);

        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isTrue();
    }

    @Test
    void testIntermediateCA(@TempDir Path tempDir) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(DatanodeTestUtils.tempDirectories(tempDir), "foobar", this.eventBus);
        datanodeKeystore.create( DatanodeTestUtils.generateKeyPair(Duration.ofDays(30)));

        final KeyPair rootCa = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("root")
                .isCA(true)
                .validity(Duration.ofDays(365)));

        final KeyPair intermediate = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.signed("intermediate", rootCa)
                .isCA(true)
                .validity(Duration.ofDays(365)));

        final KeyPair server = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.signed("server", intermediate)
                .isCA(true)
                .validity(Duration.ofDays(365)));

        final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest("my-hostname", List.of("second-hostname"));

        final CsrSigner signer = new CsrSigner();
        final X509Certificate datanodeCert = signer.sign(server.privateKey(), server.certificate(), csr, 30);
        final CertificateChain certChain = new CertificateChain(datanodeCert, List.of(server.certificate(), intermediate.certificate(), rootCa.certificate()));

        datanodeKeystore.replaceCertificatesInKeystore(certChain);

        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isTrue();
    }

    @Test
    void testResetToSelfsignedCertificate(@TempDir Path tempDir) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(DatanodeTestUtils.tempDirectories(tempDir), "foobar", this.eventBus);

        // Initially keystore doesn't exist
        Assertions.assertThat(datanodeKeystore.exists()).isFalse();

        // Create a self-signed certificate
        final java.security.KeyStore keystore = datanodeKeystore.resetToSelfsignedCertificate();

        // Verify keystore was created and persisted
        Assertions.assertThat(datanodeKeystore.exists()).isTrue();
        Assertions.assertThat(keystore).isNotNull();

        // Verify the certificate is self-signed (not CA-signed)
        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isFalse();

        // Verify the certificate exists with the correct alias
        final X509Certificate cert = (X509Certificate) keystore.getCertificate(DatanodeKeystore.DATANODE_KEY_ALIAS);
        Assertions.assertThat(cert).isNotNull();

        // Verify it's self-signed (issuer == subject)
        Assertions.assertThat(cert.getIssuerX500Principal()).isEqualTo(cert.getSubjectX500Principal());

        // Verify subject contains the datanode alias
        Assertions.assertThat(cert.getSubjectX500Principal().getName()).contains(DatanodeKeystore.DATANODE_KEY_ALIAS);

        // Verify certificate validity is approximately 99 years (with some tolerance)
        long validityDays = (cert.getNotAfter().getTime() - cert.getNotBefore().getTime()) / (1000L * 60 * 60 * 24);
        Assertions.assertThat(validityDays).isBetween(99L * 365 - 10, 99L * 365 + 10);

        // One event should be posted for self-signed certificate creation
        Assertions.assertThat(this.receivedEvents).hasSize(1);
    }

    @Test
    void testResetToSelfsignedCertificateReplacesExistingSignedCert(@TempDir Path tempDir) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(DatanodeTestUtils.tempDirectories(tempDir), "foobar", this.eventBus);

        // Create initial keystore with a CA-signed certificate
        datanodeKeystore.create(DatanodeTestUtils.generateKeyPair(Duration.ofDays(30)));
        final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest("my-hostname", List.of());

        final CsrSigner signer = new CsrSigner();
        final KeyPair ca = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("Graylog CA").isCA(true).validity(Duration.ofDays(365)));
        final X509Certificate datanodeCert = signer.sign(ca.privateKey(), ca.certificate(), csr, 30);
        final CertificateChain certChain = new CertificateChain(datanodeCert, List.of(ca.certificate()));

        datanodeKeystore.replaceCertificatesInKeystore(certChain);

        // Verify we have a signed certificate
        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isTrue();
        Assertions.assertThat(this.receivedEvents).hasSize(1);

        // Reset to self-signed certificate
        receivedEvents.clear();
        final java.security.KeyStore keystore = datanodeKeystore.resetToSelfsignedCertificate();

        // Verify the certificate is now self-signed
        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isFalse();

        final X509Certificate cert = (X509Certificate) keystore.getCertificate(DatanodeKeystore.DATANODE_KEY_ALIAS);
        Assertions.assertThat(cert.getIssuerX500Principal()).isEqualTo(cert.getSubjectX500Principal());

        // One event should be posted when resetting to self-signed
        Assertions.assertThat(this.receivedEvents).hasSize(1);
    }
}
