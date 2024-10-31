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

import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeystoreFileStorageTest {

    private KeystoreFileStorage toTest;

    @Test
    void testKeyStoreSaveAndRetrieveWithNoPasswordChange(@TempDir Path tmpDir) throws Exception {
        toTest = new KeystoreFileStorage();
        final Path keystoreFile = tmpDir.resolve("keystore_file.p12");
        KeyStore testKeystore = KeyStore.getInstance(CertConstants.PKCS12);
        testKeystore.load(new FileInputStream("src/test/resources/org/graylog/security/certutil/keystore/storage/sample_certificate_keystore.p12"), "password".toCharArray());
        char[] passwd = "password".toCharArray();

        toTest.writeKeyStore(keystoreFile, testKeystore, passwd, null);
        final Optional<KeyStore> keyStore = toTest.readKeyStore(keystoreFile, passwd);
        assertTrue(keyStore.isPresent());
        final KeyStore keyStoreToVerify = keyStore.get();
        assertEquals(testKeystore.getCertificate("datanode"), keyStoreToVerify.getCertificate("datanode"));
        assertEquals("RSA", keyStoreToVerify.getKey("datanode", passwd).getAlgorithm());
    }

    @Test
    void testKeyStoreSaveAndRetrieveWithPasswordChange(@TempDir Path tmpDir) throws Exception {
        toTest = new KeystoreFileStorage();
        final Path keystoreFile = tmpDir.resolve("keystore_file.p12");
        KeyStore testKeystore = KeyStore.getInstance(CertConstants.PKCS12);
        testKeystore.load(new FileInputStream("src/test/resources/org/graylog/security/certutil/keystore/storage/sample_certificate_keystore.p12"), "password".toCharArray());
        char[] passwd = "password".toCharArray();
        char[] newPasswd = "secret".toCharArray();

        toTest.writeKeyStore(keystoreFile, testKeystore, passwd, newPasswd);
        final Optional<KeyStore> keyStore = toTest.readKeyStore(keystoreFile, newPasswd);
        assertTrue(keyStore.isPresent());
        final KeyStore keyStoreToVerify = keyStore.get();
        assertEquals(testKeystore.getCertificate("datanode"), keyStoreToVerify.getCertificate("datanode"));
        assertEquals("RSA", keyStoreToVerify.getKey("datanode", newPasswd).getAlgorithm());
        assertThrows(UnrecoverableKeyException.class, () -> keyStoreToVerify.getKey("datanode", passwd));

    }

    @Test
    void testKeystoreReadThrowsExceptionWhenUsingWrongPassword(@TempDir Path tmpDir) throws Exception {
        toTest = new KeystoreFileStorage();
        final Path keystoreFile = tmpDir.resolve("keystore_file.p12");
        KeyStore testKeystore = KeyStore.getInstance(CertConstants.PKCS12);
        testKeystore.load(null, null);
        char[] passwd = "password".toCharArray();

        toTest.writeKeyStore(keystoreFile, testKeystore, passwd, null);
        assertThrows(KeyStoreStorageException.class, () -> toTest.readKeyStore(keystoreFile, "wrong password".toCharArray()));
    }

}
