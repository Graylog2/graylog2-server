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
package org.graylog2.shared.security.tls;

import com.google.common.io.Resources;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

public class PemKeyStoreTest {

    @Before
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testGenerateKeySpec() throws Exception {
        final URL url = Resources.getResource("org/graylog2/shared/security/tls/private.key");
        final byte[] privateKey = PemReader.readPrivateKey(Paths.get(url.toURI()));

        final PKCS8EncodedKeySpec keySpec = PemKeyStore.generateKeySpec(null, privateKey);

        assertThat(keySpec.getFormat()).isEqualTo("PKCS#8");
        assertThat(keySpec.getEncoded()).isEqualTo(privateKey);
    }

    @Test
    public void testGenerateKeySpecFromPBE1EncryptedPrivateKey() throws Exception {
        final URL url = Resources.getResource("org/graylog2/shared/security/tls/key-enc-pbe1.p8");
        final byte[] privateKey = PemReader.readPrivateKey(Paths.get(url.toURI()));

        final PKCS8EncodedKeySpec keySpec = PemKeyStore.generateKeySpec("password".toCharArray(), privateKey);

        assertThat(keySpec.getFormat()).isEqualTo("PKCS#8");
        assertThat(keySpec.getEncoded()).isNotEmpty();
    }

    @Test
    public void testGenerateKeySpecFromPBE2EncryptedPrivateKey() throws Exception {
        final URL url = Resources.getResource("org/graylog2/shared/security/tls/key-enc-pbe2-sha256.p8");
        final byte[] privateKey = PemReader.readPrivateKey(Paths.get(url.toURI()));

        final PKCS8EncodedKeySpec keySpec = PemKeyStore.generateKeySpec("password".toCharArray(), privateKey);

        assertThat(keySpec.getFormat()).isEqualTo("PKCS#8");
        assertThat(keySpec.getEncoded()).isNotEmpty();
    }

    @Test
    public void testBuildKeyStore() throws Exception {
        final Path certChainFile = Paths.get(Resources.getResource("org/graylog2/shared/security/tls/chain.crt").toURI());
        final Path keyFile = Paths.get(Resources.getResource("org/graylog2/shared/security/tls/private.key").toURI());
        final KeyStore keyStore = PemKeyStore.buildKeyStore(certChainFile, keyFile, null);

        final Certificate[] keys = keyStore.getCertificateChain("key");
        assertThat(keys).hasSize(2);

        final Key key = keyStore.getKey("key", new char[0]);
        assertThat(key.getFormat()).isEqualTo("PKCS#8");
        assertThat(key.getEncoded()).isNotEmpty();
    }

    @Test
    public void testBuildKeyStoreWithSecuredPrivateKey() throws Exception {
        final Path certChainFile = Paths.get(Resources.getResource("org/graylog2/shared/security/tls/chain.crt").toURI());
        final Path keyFile = Paths.get(Resources.getResource("org/graylog2/shared/security/tls/key-enc-pbe1.p8").toURI());
        final KeyStore keyStore = PemKeyStore.buildKeyStore(certChainFile, keyFile, "password".toCharArray());

        final Certificate[] keys = keyStore.getCertificateChain("key");
        assertThat(keys).hasSize(2);

        final Key key = keyStore.getKey("key", "password".toCharArray());
        assertThat(key.getFormat()).isEqualTo("PKCS#8");
        assertThat(key.getEncoded()).isNotEmpty();
    }

    @Test
    public void testBuildKeyStoreWithPBES2EncryptedPrivateKey() throws Exception {
        final Path certChainFile = Paths.get(Resources.getResource("org/graylog2/shared/security/tls/chain.crt").toURI());
        final Path keyFile = Paths.get(Resources.getResource("org/graylog2/shared/security/tls/key-enc-pbe2-sha256.p8").toURI());
        final KeyStore keyStore = PemKeyStore.buildKeyStore(certChainFile, keyFile, "password".toCharArray());

        final Certificate[] keys = keyStore.getCertificateChain("key");
        assertThat(keys).hasSize(2);

        final Key key = keyStore.getKey("key", "password".toCharArray());
        assertThat(key.getFormat()).isEqualTo("PKCS#8");
        assertThat(key.getEncoded()).isNotEmpty();
    }
}
