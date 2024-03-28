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
package org.graylog2.plugin.inputs.transports.util;

import org.graylog.testing.ResourceUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyUtilNonParameterizedTest {
    @Before
    public void init() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Test
    // Read a PKCS5 v2.0 encrypted private key in PKCS8 format
    public void testPrivateKeyFromProtectedFile() throws URISyntaxException, KeyUtilException {
        final String password = "test";
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.pem.pkcs8.protected"), password);
        assertThat(privateKey).isNotNull();
    }

    @Test
    // Read a PKCS5 v1.5 encrypted private key in PKCS8 format
    public void testPrivateKeyFromLegacyProtectedFile() throws URISyntaxException, KeyUtilException {
        final String password = "test";
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.pem.pkcs8.v1.protected"), password);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testPrivateKeyFromUnprotectedFile() throws URISyntaxException, KeyUtilException {
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.pem.pkcs8.unprotected"), null);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testPrivateKeyFromPKCS1() throws URISyntaxException, KeyUtilException {
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.unprotected.pkcs1"), null);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testGeneratePKSC8PrivateKey() throws GeneralSecurityException, URISyntaxException, KeyUtilException {
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.pem.pkcs8.unprotected"), null);
        final String tmpPassword = "dummypassword";
        final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        File pkcs8EncryptedKeyFile = KeyUtil.generatePKCS8FromPrivateKey(tmpDir, tmpPassword.toCharArray(), privateKey);
        final PrivateKey retrievedKey = fileToKey(pkcs8EncryptedKeyFile, tmpPassword);
        assertThat(retrievedKey).hasToString(privateKey.toString());
    }

    private File resourceToFile(String fileName) throws URISyntaxException {
        return ResourceUtil.resourceToTmpFile("org/graylog2/plugin/inputs/transports/util/" + fileName);
    }

    private PrivateKey fileToKey(File keyFile, String password) throws KeyUtilException {
        return KeyUtil.privateKeyFromFile(password, keyFile);
    }
}
