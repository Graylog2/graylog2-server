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
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TruststoreCreatorTest {

    @Test
    void testTrustStoreCreation(@TempDir Path tempDir) throws Exception {

        final FilesystemKeystoreInformation root = createKeystore(tempDir.resolve("root.p12"), "root", "CN=ROOT", BigInteger.ONE);
        final FilesystemKeystoreInformation boot = createKeystore(tempDir.resolve("boot.p12"), "boot", "CN=BOOT", BigInteger.TWO);

        final FilesystemKeystoreInformation truststore = TruststoreCreator.newEmpty()
                .addRootCert("root", root, "root")
                .addRootCert("boot", boot, "boot")

                .persist(tempDir.resolve("truststore.sec"), "caramba! caramba!".toCharArray());

        assertTrue(truststore.location().toFile().exists());

        KeystoreFileStorage keystoreFileStorage = new KeystoreFileStorage();
        final Optional<KeyStore> keyStoreOptional = keystoreFileStorage.readKeyStore(truststore.location(), truststore.password());

        assertTrue(keyStoreOptional.isPresent());

        final KeyStore keyStore = keyStoreOptional.get();
        assertThat(ImmutableList.copyOf(keyStore.aliases().asIterator()))
                .containsOnly("root", "boot");

        final Certificate rootCert = keyStore.getCertificate("root");
        verifyCertificate(rootCert, "CN=ROOT", BigInteger.ONE);
        final Certificate bootCert = keyStore.getCertificate("boot");
        verifyCertificate(bootCert, "CN=BOOT", BigInteger.TWO);
    }

    @Test
    void testDefaultJvm() throws KeyStoreException {
        final TruststoreCreator truststoreCreator = TruststoreCreator.newDefaultJvm();
        final KeyStore truststore = truststoreCreator.getTruststore();
        final ArrayList<String> aliases = Lists.newArrayList(truststore.aliases().asIterator());
        Assertions.assertThat(aliases)
                .hasSizeGreaterThan(10); // reasonable assumption that there are more than 10 trusted CAs in JVM truststore?
    }

    @Test
    void testAdditionalCertificates(@TempDir Path tempDir) throws GeneralSecurityException, IOException, OperatorCreationException {
        final FilesystemKeystoreInformation root = createKeystore(tempDir.resolve("root.p12"), "something-unknown", "CN=ROOT", BigInteger.ONE);
        final X509Certificate cert = (X509Certificate) root.loadKeystore().getCertificate("something-unknown");

        final FilesystemKeystoreInformation truststore = TruststoreCreator.newEmpty()
                .addCertificates(List.of(cert))
                .persist(tempDir.resolve("truststore.sec"), "caramba! caramba!".toCharArray());

        final KeyStore keystore = truststore.loadKeystore();

        final String alias = keystore.getCertificateAlias(cert);
        Assertions.assertThat(alias)
                .isNotNull()
                .isEqualTo("cn=root");

    }

    private void verifyCertificate(final Certificate rootCert, final String cnName, final BigInteger serialNumber) {
        assertThat(rootCert)
                .isNotNull()
                .isInstanceOf(X509Certificate.class);

        final X509Certificate x509Certificate = (X509Certificate) rootCert;
        assertEquals(serialNumber, x509Certificate.getSerialNumber());
        assertEquals(cnName, x509Certificate.getIssuerX500Principal().getName());
    }

    private FilesystemKeystoreInformation createKeystore(Path path, String alias, final String cnName, final BigInteger serialNumber) throws GeneralSecurityException, OperatorCreationException, IOException {
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

        final X509Certificate signedCert = new JcaX509CertificateConverter().getCertificate(certHolder);

        KeyStore trustStore = KeyStore.getInstance(CertConstants.PKCS12);
        trustStore.load(null, null);

        final char[] password = RandomStringUtils.randomAlphabetic(256).toCharArray();

        trustStore.setKeyEntry(alias, certKeyPair.getPrivate(), password, new Certificate[]{signedCert});


        try (final FileOutputStream fileOutputStream = new FileOutputStream(path.toFile())) {
            trustStore.store(fileOutputStream, password);
        }
        return new FilesystemKeystoreInformation(path, password);
    }
}
