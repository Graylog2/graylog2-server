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

package org.graylog.datanode.process.configuration.files;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.graylog.security.certutil.CertConstants.PKCS12;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class KeystoreConfigFileTest {

    KeyStore originalKeystore;
    KeystoreInformation keystoreInformation;
    KeystoreConfigFile keystoreConfigFile;

    @Test
    void noPasswordThrowsException() throws Exception {
        initializeKeystore("");
        OutputStream os = mock(OutputStream.class);
        assertThatThrownBy(() -> keystoreConfigFile.write(os)).cause().hasMessageContaining("Keystore password is empty!");
    }

    @Test
    void emptyKeystoreWritten() throws Exception {
        String password = "password";
        initializeKeystore(password);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        keystoreConfigFile.write(os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        KeyStore result = KeyStore.getInstance(PKCS12);
        result.load(is, "password".toCharArray());
        assertThat(Collections.list(result.aliases())).isEqualTo(Collections.list(originalKeystore.aliases()));
    }

    @Test
    void validCertificatesWritten() throws Exception {
        String password = "password";
        initializeKeystore(password);

        addCertificateToKeystore("valid1",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        );
        addCertificateChainToKeystore("valid2",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
                password
        );

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        keystoreConfigFile.write(os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        KeyStore result = KeyStore.getInstance(PKCS12);
        result.load(is, "password".toCharArray());
        ArrayList<String> aliases = Collections.list(result.aliases());
        assertThat(aliases).hasSize(2);
        assertThat(aliases).containsExactlyInAnyOrder("valid1", "valid2");
    }

    @Test
    void onlyValidCertificateChainsWritten() throws Exception {
        String password = "password";
        initializeKeystore(password);

        addCertificateToKeystore("valid1",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS))
        );
        addCertificateChainToKeystore("valid2",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
                password
        );
        addCertificateChainToKeystore("expired",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().minus(5, ChronoUnit.DAYS)),
                password
        );
        addCertificateChainToKeystore("notyetvalid",
                Date.from(Instant.now().plus(5, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
                password
        );

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        keystoreConfigFile.write(os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        KeyStore result = KeyStore.getInstance(PKCS12);
        result.load(is, "password".toCharArray());
        ArrayList<String> aliases = Collections.list(result.aliases());
        assertThat(aliases).hasSize(2);
        assertThat(aliases).containsExactlyInAnyOrder("valid1", "valid2");
    }

    @Test
    void invalidElementsRemoved() throws Exception {
        String password = "password";
        initializeKeystore(password);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        keystoreConfigFile.write(os);
    }

    private void initializeKeystore(String password) throws Exception {
        originalKeystore = KeyStore.getInstance(PKCS12);
        originalKeystore.load(null, password.toCharArray());
        keystoreInformation = new InMemoryKeystoreInformation(originalKeystore, password.toCharArray());
        keystoreConfigFile = new KeystoreConfigFile(mock(Path.class), keystoreInformation);
    }

    private void addCertificateToKeystore(String alias, Date validFrom, Date validTo) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name rootSubject = new X500Name("CN=" + alias);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(
                rootSubject,
                BigInteger.valueOf(1),
                validFrom,
                validTo,
                rootSubject,
                keyPair.getPublic()
        );
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(rootCertBuilder.build(signer));
        originalKeystore.setCertificateEntry(alias, cert);
    }

    private void addCertificateChainToKeystore(String alias, Date validFrom, Date validTo, String password) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair rootKeyPair = keyPairGenerator.generateKeyPair();
        KeyPair intermediateKeyPair = keyPairGenerator.generateKeyPair();
        KeyPair testKeyPair = keyPairGenerator.generateKeyPair();

        X500Name rootSubject = new X500Name("CN=Root CA" + alias);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(rootKeyPair.getPrivate());
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(
                rootSubject,
                BigInteger.valueOf(1),
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
                rootSubject,
                rootKeyPair.getPublic()
        );
        X509Certificate rootCert = new JcaX509CertificateConverter().getCertificate(rootCertBuilder.build(signer));

        X500Name intermediateSubject = new X500Name("CN=Test Intermediate CA");
        ContentSigner intermediateSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(rootKeyPair.getPrivate());
        X509v3CertificateBuilder intermediateCertBuilder = new JcaX509v3CertificateBuilder(
                rootSubject,
                BigInteger.valueOf(2),
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
                intermediateSubject,
                intermediateKeyPair.getPublic()
        );
        X509Certificate intermediateCert = new JcaX509CertificateConverter().getCertificate(intermediateCertBuilder.build(intermediateSigner));

        // Create End-entity Certificate
        X500Name testSubject = new X500Name("CN=" + alias);
        ContentSigner testSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(intermediateKeyPair.getPrivate());
        X509v3CertificateBuilder testCertBuilder = new JcaX509v3CertificateBuilder(
                intermediateSubject,
                BigInteger.valueOf(3),
                validFrom,
                validTo,
                testSubject,
                testKeyPair.getPublic()
        );
        X509Certificate testCert = new JcaX509CertificateConverter().getCertificate(testCertBuilder.build(testSigner));

        // Step 3: Assemble the Certificate Chain
        X509Certificate[] chain = {testCert, intermediateCert, rootCert};
        originalKeystore.setKeyEntry(alias, testKeyPair.getPrivate(), password.toCharArray(), chain);
    }


}
