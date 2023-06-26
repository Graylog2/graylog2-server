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
package org.graylog.security.certutil.privatekey;

import org.bouncycastle.pkcs.PKCSException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrivateKeyEncryptedFileStorageTest {

    @Test
    void testKeyStorageSaveAndRetrieve(@TempDir Path tmpDir) throws Exception {
        PrivateKeyEncryptedFileStorage privateKeyEncryptedFileStorage = new PrivateKeyEncryptedFileStorage(tmpDir.resolve("temp.key").toString());
        char[] passwd = "password".toCharArray();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        java.security.KeyPair certKeyPair = keyGen.generateKeyPair();
        final PrivateKey privateKey = certKeyPair.getPrivate();

        privateKeyEncryptedFileStorage.writeEncryptedKey(passwd, privateKey);
        final PrivateKey readPrivateKey = privateKeyEncryptedFileStorage.readEncryptedKey(passwd);

        assertEquals(privateKey, readPrivateKey);
    }

    @Test
    void testKeyStorageThrowsExceptionWhenUsingWrongPasswordDuringRead(@TempDir Path tmpDir) throws Exception {
        PrivateKeyEncryptedFileStorage privateKeyEncryptedFileStorage = new PrivateKeyEncryptedFileStorage(tmpDir.resolve("temp.key").toString());
        char[] passwd = "password".toCharArray();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        java.security.KeyPair certKeyPair = keyGen.generateKeyPair();
        final PrivateKey privateKey = certKeyPair.getPrivate();

        privateKeyEncryptedFileStorage.writeEncryptedKey(passwd, privateKey);
        assertThrows(PKCSException.class, () -> privateKeyEncryptedFileStorage.readEncryptedKey("wrong password".toCharArray()));
    }
}
