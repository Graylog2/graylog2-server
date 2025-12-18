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
package org.graylog2.security;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TruststoreCreatorTest {

    private static final CertificateGenerator CERTIFICATE_GENERATOR = new CertificateGenerator(1024);

    @Test
    void testTrustStoreCreation(@TempDir Path tempDir) throws Exception {

        final KeystoreInformation root = createKeystore("root", "CN=ROOT", BigInteger.ONE);
        final KeystoreInformation boot = createKeystore("boot", "CN=BOOT", BigInteger.TWO);

        final FilesystemKeystoreInformation truststore = TruststoreCreator.newEmpty()
                .addCertificates(root)
                .addCertificates(boot)

                .persist(tempDir.resolve("truststore.sec"), "caramba! caramba!".toCharArray());

        assertTrue(truststore.location().toFile().exists());

        KeystoreFileStorage keystoreFileStorage = new KeystoreFileStorage();
        final Optional<KeyStore> keyStoreOptional = keystoreFileStorage.readKeyStore(truststore.location(), truststore.password());

        assertTrue(keyStoreOptional.isPresent());

        final KeyStore keyStore = keyStoreOptional.get();
        assertThat(ImmutableList.copyOf(keyStore.aliases().asIterator()))
                .containsOnly("cn=root", "cn=boot");

        final Certificate rootCert = keyStore.getCertificate("cn=root");
        verifyCertificate(rootCert, "CN=ROOT", BigInteger.ONE);
        final Certificate bootCert = keyStore.getCertificate("cn=boot");
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
    void testAdditionalCertificates(@TempDir Path tempDir) throws Exception {
        final KeystoreInformation root = createKeystore("something-unknown", "CN=ROOT", BigInteger.ONE);
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

    @Test
    void testIntermediateCa() throws Exception {
        final KeyPair ca = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("my-ca").isCA(true).validity(Duration.ofDays(100)));
        final KeyPair intermediateCa = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.signed("intermediate", ca).isCA(true).validity(Duration.ofDays(100)));
        final KeyPair nodeKeys = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.signed("my-node", intermediateCa).isCA(false).validity(Duration.ofDays(100)));


        final InMemoryKeystoreInformation keystoreInformation = createInMemoryKeystore(nodeKeys, intermediateCa);

        final KeyStore truststore = TruststoreCreator.newEmpty()
                .addCertificates(keystoreInformation)
                .getTruststore();

        final X509TrustManager defaultTrustManager = createTrustManager(truststore);

        Assertions.assertThatNoException().isThrownBy(() -> defaultTrustManager.checkServerTrusted(new X509Certificate[]{nodeKeys.certificate()}, "RSA"));

        final KeyPair fakeNodeKeys = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("my-fake-node").isCA(false).validity(Duration.ofDays(100)));
        Assertions.assertThatThrownBy(() -> defaultTrustManager.checkServerTrusted(new X509Certificate[]{fakeNodeKeys.certificate()}, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void testDuplicateCname() throws Exception {
        final KeyPair ca1 = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("my-ca").isCA(true).validity(Duration.ofDays(90)));
        final KeyPair ca2 = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("my-ca").isCA(true).validity(Duration.ofDays(90)));
        final KeyPair ca3 = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("my-ca").isCA(true).validity(Duration.ofDays(90)));

        final KeyStore truststore = TruststoreCreator.newEmpty()
                .addCertificates(List.of(ca1.certificate()))
                .addCertificates(List.of(ca2.certificate()))
                .addCertificates(List.of(ca3.certificate()))
                .getTruststore();

        Assertions.assertThat(Collections.list(truststore.aliases()))
                .hasSize(3)
                .contains("cn=my-ca")
                .contains("cn=my-ca_1")
                .contains("cn=my-ca_2");
    }

    private static X509TrustManager createTrustManager(KeyStore caTruststore) throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(caTruststore);
        final TrustManager[] trustManagers = tmf.getTrustManagers();
        return (X509TrustManager) trustManagers[0];
    }

    @Test
    void testExpiredCertificates() throws Exception {

        final KeyStore keystore = KeyStore.getInstance(CertConstants.PKCS12);
        keystore.load(null, null);

        keystore.setCertificateEntry("valid1", certWithValidity("valid1",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        ));
        keystore.setCertificateEntry("valid2", certWithValidity("valid2",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        ));
        keystore.setCertificateEntry("expired", certWithValidity("expired",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().minus(5, ChronoUnit.DAYS))
        ));
        keystore.setCertificateEntry("notyetvalid", certWithValidity("notyetvalid",
                Date.from(Instant.now().plus(5, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        ));

        final TruststoreCreator truststoreCreator = new TruststoreCreator(keystore);
        Assertions.assertThat(Collections.list(truststoreCreator.getTruststore().aliases()))
                .hasSize(2)
                .contains("valid1", "valid2");
    }

    @Test
    void testAddingExpiredCertificates() throws Exception {

        final TruststoreCreator truststoreCreator = TruststoreCreator.newEmpty();

        truststoreCreator.addCertificates(List.of(certWithValidity("valid1",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        )));

        truststoreCreator.addCertificates(List.of(certWithValidity("valid2",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        )));

        truststoreCreator.addCertificates(List.of(certWithValidity("expired",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().minus(5, ChronoUnit.DAYS))
        )));

        truststoreCreator.addCertificates(List.of(certWithValidity("notyetvalid",
                Date.from(Instant.now().plus(5, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        )));

        Assertions.assertThat(Collections.list(truststoreCreator.getTruststore().aliases()))
                .hasSize(2)
                .contains("cn=valid1", "cn=valid2");
    }

    private X509Certificate certWithValidity(String alias, Date notBefore, Date notAfter) throws Exception {
        final KeyPair keyPair = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned(alias).isCA(true).validity(Duration.ofDays(365)));

        X500Name rootSubject = new X500Name("CN=" + alias);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.privateKey());
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(
                rootSubject,
                BigInteger.valueOf(1),
                notBefore,
                notAfter,
                rootSubject,
                keyPair.publicKey()
        );
        return new JcaX509CertificateConverter().getCertificate(rootCertBuilder.build(signer));
    }


    @SuppressWarnings("deprecation")
    @Nonnull
    private static InMemoryKeystoreInformation createInMemoryKeystore(KeyPair nodeKeys, KeyPair intermediate) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        final char[] password = RandomStringUtils.randomAlphabetic(256).toCharArray();
        KeyStore keystore = KeyStore.getInstance(CertConstants.PKCS12);
        keystore.load(null, null);
        keystore.setKeyEntry("my-node", nodeKeys.privateKey(), password, new Certificate[]{nodeKeys.certificate(), intermediate.certificate()});
        return new InMemoryKeystoreInformation(keystore, password);
    }

    private void verifyCertificate(final Certificate rootCert, final String cnName, final BigInteger serialNumber) {
        assertThat(rootCert)
                .isNotNull()
                .isInstanceOf(X509Certificate.class);

        final X509Certificate x509Certificate = (X509Certificate) rootCert;
        assertEquals(serialNumber, x509Certificate.getSerialNumber());
        assertEquals(cnName, x509Certificate.getIssuerX500Principal().getName());
    }

    @SuppressWarnings("deprecation")
    private KeystoreInformation createKeystore(String alias, final String cnName, final BigInteger serialNumber) throws IOException, CertificateException, OperatorCreationException, KeyStoreException, NoSuchAlgorithmException {
        final KeyPair keyPair = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned(alias).isCA(true).validity(Duration.ofDays(90)));

        X500Name name = new X500Name(cnName);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name,
                serialNumber,
                Date.from(Instant.now()),
                Date.from(Instant.now().plus(13, ChronoUnit.DAYS)),
                name,
                keyPair.publicKey());

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM).build(keyPair.privateKey());
        X509CertificateHolder certHolder = builder.build(signer);

        final X509Certificate signedCert = new JcaX509CertificateConverter().getCertificate(certHolder);

        KeyStore keyStore = KeyStore.getInstance(CertConstants.PKCS12);
        keyStore.load(null, null);

        final char[] password = RandomStringUtils.randomAlphabetic(256).toCharArray();

        keyStore.setKeyEntry(alias, keyPair.privateKey(), password, new Certificate[]{signedCert});

        return new InMemoryKeystoreInformation(keyStore, password);
    }
}
