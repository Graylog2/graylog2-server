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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KeyUtilTest {
    @Before
    public void init() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private static final ImmutableMap<String, String> CERTIFICATES = ImmutableMap.of(
            "RSA", "server.crt.rsa",
            "DSA", "server.crt.dsa",
            "ECDSA", "server.crt.ecdsa"
    );

    @Parameterized.Parameters(name = "{0} with file <{1}>, password <{2}>")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // algorithm, key filename, password, exception class, exception text
                {"RSA", "server.key.pem.ue.pkcs1.rsa", null, null, null},
                {"RSA", "server.key.pem.e.pkcs8.rsa", "test", null, null},
                {"RSA", "server.key.pem.ue.pkcs8.rsa", null, null, null},
                {"RSA", "server.key.der.e.pkcs8.rsa", "test", KeyUtilException.class, "No key found in PEM file"},
                {"RSA", "server.key.der.ue.pkcs8.rsa", null, KeyUtilException.class, "No key found in PEM file"},

                {"DSA", "server.key.pem.ue.pkcs1.dsa", null, null, null},
                {"DSA", "server.key.pem.e.pkcs8.dsa", "test", null, null},
                {"DSA", "server.key.pem.ue.pkcs8.dsa", null, null, null},
                {"DSA", "server.key.der.e.pkcs8.dsa", "test", KeyUtilException.class, "No key found in PEM file"},
                {"DSA", "server.key.der.ue.pkcs8.dsa", null, KeyUtilException.class, "No key found in PEM file"},

                {"ECDSA", "server.key.pem.ue.pkcs1.ecdsa", null, null, null},
                {"ECDSA", "server.key.pem.e.pkcs8.ecdsa", "test", null, null},
                {"ECDSA", "server.key.pem.ue.pkcs8.ecdsa", null, null, null},
                {"ECDSA", "server.key.der.e.pkcs8.ecdsa", "test", KeyUtilException.class, "No key found in PEM file"},
                {"ECDSA", "server.key.der.ue.pkcs8.ecdsa", null, KeyUtilException.class, "No key found in PEM file"},

                {"RSA", "server.key.invalid", null, KeyUtilException.class, "No key found in PEM file"},
        });
    }

    private final String keyAlgorithm;
    private final String keyFileName;
    private final String keyPassword;
    private final Class<? extends Exception> exceptionClass;
    private final String exceptionMessage;

    public KeyUtilTest(String keyAlgorithm,
                       String keyFileName,
                       String keyPassword,
                       Class<? extends Exception> exceptionClass,
                       String exceptionMessage) {
        this.keyAlgorithm = requireNonNull(keyAlgorithm);
        this.keyFileName = requireNonNull(keyFileName);
        this.keyPassword = keyPassword;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

    private File resourceToFile(String fileName) throws URISyntaxException {
        return new File(Resources.getResource("org/graylog2/plugin/inputs/transports/util/" + fileName).toURI());
    }

    @Test
    public void testLoadCertificates() throws Exception {
        final File certFile = resourceToFile(CERTIFICATES.get(keyAlgorithm));
        final Collection<? extends Certificate> certificates = KeyUtil.loadCertificates(certFile.toPath());
        assertThat(certificates)
                .isNotEmpty()
                .hasOnlyElementsOfType(X509Certificate.class);
    }

    @Test
    public void testLoadCertificatesDir() throws Exception {
        final File certDir = resourceToFile("certs");
        final Collection<? extends Certificate> certificates = KeyUtil.loadCertificates(certDir.toPath());
        assertThat(certificates)
                .isNotEmpty()
                .hasSize(2)
                .hasOnlyElementsOfType(X509Certificate.class);
    }

    @Test
    public void testLoadPrivateKey() throws Exception {
        if (exceptionClass != null) {
            expectedException.expect(exceptionClass);
            expectedException.expectMessage(exceptionMessage);
        }

        final File keyFile = resourceToFile(keyFileName);
        final PrivateKey privateKey = KeyUtil.privateKeyFromFile(keyPassword, keyFile);
        assertThat(privateKey).isNotNull();
    }
}
