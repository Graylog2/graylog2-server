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

import com.google.common.collect.ImmutableList;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.graylog.security.certutil.keystore.storage.KeystoreContentMover;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;


public class TruststoreCreatorTest {

    private TruststoreCreator toTest;

    @BeforeEach
    void setUp() {
        toTest = new TruststoreCreator();
    }

    @Test
    void testTrustStoreCreation(@TempDir Path tempDir) throws Exception {
        final Path trustStoreFile = tempDir.resolve("truststore.sec");
        final char[] truststorePassword = "caramba! caramba!".toCharArray();

        toTest.createTruststore(
                Map.of(
                        "root", prepareSimpleCert("CN=ROOT", BigInteger.ONE),
                        "boot", prepareSimpleCert("CN=BOOT", BigInteger.TWO)
                ),
                truststorePassword,
                trustStoreFile);

        assertTrue(trustStoreFile.toFile().exists());

        KeystoreFileStorage keystoreFileStorage = new KeystoreFileStorage(mock(KeystoreContentMover.class));
        final Optional<KeyStore> keyStoreOptional = keystoreFileStorage.readKeyStore(new KeystoreFileLocation(trustStoreFile), truststorePassword);

        assertTrue(keyStoreOptional.isPresent());

        final KeyStore keyStore = keyStoreOptional.get();
        assertThat(ImmutableList.copyOf(keyStore.aliases().asIterator()))
                .containsOnly("root", "boot");

        final Certificate rootCert = keyStore.getCertificate("root");
        verifyCertificate(rootCert, "CN=ROOT", BigInteger.ONE);
        final Certificate bootCert = keyStore.getCertificate("boot");
        verifyCertificate(bootCert, "CN=BOOT", BigInteger.TWO);

    }

    private void verifyCertificate(final Certificate rootCert, final String cnName, final BigInteger serialNumber) {
        assertThat(rootCert)
                .isNotNull()
                .isInstanceOf(X509Certificate.class);

        final X509Certificate x509Certificate = (X509Certificate) rootCert;
        assertEquals(serialNumber, x509Certificate.getSerialNumber());
        assertEquals(cnName, x509Certificate.getIssuerX500Principal().getName());
    }

    private X509Certificate prepareSimpleCert(final String cnName, final BigInteger serialNumber) throws GeneralSecurityException, OperatorCreationException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        java.security.KeyPair certKeyPair = keyGen.generateKeyPair();
        X500Name name = new X500Name(cnName);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name,
                serialNumber,
                Date.from(Instant.now()),
                Date.from(Instant.now().plus(13, ChronoUnit.DAYS)),
                name,
                certKeyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM).build(certKeyPair.getPrivate());
        X509CertificateHolder certHolder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);

    }

}
