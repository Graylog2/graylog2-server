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
package org.graylog.security.certutil.keystore.storage;

import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.graylog.security.certutil.CertConstants.PKCS12;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KeystoreUtilsTest {

    @Test
    void testThrowsExceptionIfNewPasswordIsNull() throws Exception {
        KeyStore originalKeyStore = KeyStore.getInstance(PKCS12);
        assertThrows(IllegalArgumentException.class, () -> KeystoreUtils.newStoreCopyContent(originalKeyStore, "nvmd".toCharArray(), null));
    }

    @Test
    void testDifferentEntriesMoving() throws Exception {
        final char[] oldPassword = "oldPass".toCharArray();
        final char[] newPassword = "newPass".toCharArray();
        KeyStore originalKeyStore = KeyStore.getInstance(PKCS12);
        originalKeyStore.load(null, oldPassword);

        final byte[] originalSecretKey = {0x54, 0x68, 0x61, 0x74, 0x73, 0x20, 0x6D, 0x79, 0x20, 0x4B, 0x75, 0x6E, 0x67, 0x20, 0x46, 0x75};
        KeyStore.SecretKeyEntry secret
                = new KeyStore.SecretKeyEntry(new SecretKeySpec(originalSecretKey,
                "AES"));
        originalKeyStore.setEntry("secretEntry", secret, new KeyStore.PasswordProtection(oldPassword));
        CertRequest req = CertRequest.selfSigned("darknet.net")
                .validity(Duration.ZERO);
        final X509Certificate trustedCA = CertificateGenerator.generate(req).certificate();
        originalKeyStore.setCertificateEntry("trusted-certificate", trustedCA);
        req = CertRequest.selfSigned("localhost")
                .validity(Duration.ZERO);
        final KeyPair keyPair = CertificateGenerator.generate(req);
        originalKeyStore.setKeyEntry("privkey", keyPair.privateKey(), oldPassword, new Certificate[]{keyPair.certificate()});

        final KeyStore newKeyStore = KeystoreUtils.newStoreCopyContent(originalKeyStore, oldPassword, newPassword);
        final KeyStore.Entry secretRetrieved = newKeyStore.getEntry("secretEntry", new KeyStore.PasswordProtection(newPassword));
        final Certificate certificateRetrieved = newKeyStore.getCertificate("trusted-certificate");
        final Key privkeyRetrieved = newKeyStore.getKey("privkey", newPassword);

        //Verify if all 3 entry types have been moved without any changes
        assertArrayEquals(originalSecretKey, ((KeyStore.SecretKeyEntry) secretRetrieved).getSecretKey().getEncoded());
        assertEquals(trustedCA, certificateRetrieved);
        assertEquals(keyPair.privateKey(), privkeyRetrieved);

    }

    @Test
    void testMovingManyEntiresOfTheSameType() throws Exception {
        final char[] oldPassword = "oldPass".toCharArray();
        final char[] newPassword = "newPass".toCharArray();
        KeyStore originalKeyStore = KeyStore.getInstance(PKCS12);
        originalKeyStore.load(null, oldPassword);

        CertRequest req = CertRequest.selfSigned("localhost")
                .validity(Duration.ZERO);
        final KeyPair keyPair1 = CertificateGenerator.generate(req);
        originalKeyStore.setKeyEntry("privkey1", keyPair1.privateKey(), oldPassword, new Certificate[]{keyPair1.certificate()});
        final KeyPair keyPair2 = CertificateGenerator.generate(req);
        originalKeyStore.setKeyEntry("privkey2", keyPair2.privateKey(), oldPassword, new Certificate[]{keyPair2.certificate()});
        final KeyPair keyPair3 = CertificateGenerator.generate(req);
        originalKeyStore.setKeyEntry("privkey3", keyPair3.privateKey(), oldPassword, new Certificate[]{keyPair3.certificate()});

        final KeyStore newKeyStore = KeystoreUtils.newStoreCopyContent(originalKeyStore, oldPassword, newPassword);
        assertEquals(keyPair1.privateKey(), newKeyStore.getKey("privkey1", newPassword));
        assertEquals(keyPair2.privateKey(), newKeyStore.getKey("privkey2", newPassword));
        assertEquals(keyPair3.privateKey(), newKeyStore.getKey("privkey3", newPassword));
    }

    @Test
    void testMatchingPrivatePublicKeysvalid() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyGen.initialize(2048);
        final java.security.KeyPair keyPair = keyGen.genKeyPair();
        Assertions.assertThat(KeystoreUtils.matchingKeys(keyPair.getPrivate(), keyPair.getPublic())).isTrue();
    }

    @Test
    void testMatchingPrivatePublicKeysInvalid() throws Exception {
        KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyGen1.initialize(2048);
        final java.security.KeyPair keyPair1 = keyGen1.genKeyPair();
        KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyGen2.initialize(2048);
        final java.security.KeyPair keyPair2 = keyGen2.genKeyPair();
        //mixing keys from different pairs
        Assertions.assertThat(KeystoreUtils.matchingKeys(keyPair1.getPrivate(), keyPair2.getPublic())).isFalse();
    }
}
