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
import io.netty.handler.ssl.SslHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KeyUtilTest {

    private static final ImmutableMap<String, String> CERTIFICATES = ImmutableMap.of(
            "RSA", "server.crt.rsa",
            "DSA", "server.crt.dsa",
            "ECDSA", "server.crt.ecdsa"
    );

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // algorithm, key filename, password, exception class, exception text
                {"RSA", "server.key.pem.ue.pkcs1.rsa", null, IllegalArgumentException.class, "Unsupported key type PKCS#1, please convert to PKCS#8"},
                {"RSA", "server.key.pem.e.pkcs8.rsa", "test", null, null},
                {"RSA", "server.key.pem.ue.pkcs8.rsa", null, null, null},
                {"RSA", "server.key.der.e.pkcs8.rsa", "test", null, null},
                {"RSA", "server.key.der.ue.pkcs8.rsa", null, null, null},

                {"DSA", "server.key.pem.ue.pkcs1.dsa", null, IllegalArgumentException.class, "Unsupported key type PKCS#1, please convert to PKCS#8"},
                {"DSA", "server.key.pem.e.pkcs8.dsa", "test", null, null},
                {"DSA", "server.key.pem.ue.pkcs8.dsa", null, null, null},
                {"DSA", "server.key.der.e.pkcs8.dsa", "test", null, null},
                {"DSA", "server.key.der.ue.pkcs8.dsa", null, null, null},

                {"ECDSA", "server.key.pem.ue.pkcs1.ecdsa", null, IllegalArgumentException.class, "Unsupported key type PKCS#1, please convert to PKCS#8"},
                {"ECDSA", "server.key.pem.e.pkcs8.ecdsa", "test", null, null},
                {"ECDSA", "server.key.pem.ue.pkcs8.ecdsa", null, null, null},
                {"ECDSA", "server.key.der.e.pkcs8.ecdsa", "test", null, null},
                {"ECDSA", "server.key.der.ue.pkcs8.ecdsa", null, null, null},

                {"RSA", "server.key.invalid", null, IllegalArgumentException.class, "Unsupported key type: "},
        });
    }

    private File resourceToFile(String fileName) throws URISyntaxException {
        return new File(Resources.getResource("org/graylog2/plugin/inputs/transports/util/" + fileName).toURI());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0} with file <{1}>, password <{2}>")
    public void testLoadCertificates(String keyAlgorithm, String keyFileName, String keyPassword, Class<? extends Exception> exceptionClass, String exceptionMessage) throws Exception {
        final File certFile = resourceToFile(CERTIFICATES.get(keyAlgorithm));
        final Collection<? extends Certificate> certificates = KeyUtil.loadCertificates(certFile.toPath());
        assertThat(certificates)
                .isNotEmpty()
                .hasOnlyElementsOfType(X509Certificate.class);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0} with file <{1}>, password <{2}>")
    public void testLoadCertificatesDir(String keyAlgorithm, String keyFileName, String keyPassword, Class<? extends Exception> exceptionClass, String exceptionMessage) throws Exception {
        final File certDir = resourceToFile("certs");
        final Collection<? extends Certificate> certificates = KeyUtil.loadCertificates(certDir.toPath());
        assertThat(certificates)
                .isNotEmpty()
                .hasSize(2)
                .hasOnlyElementsOfType(X509Certificate.class);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0} with file <{1}>, password <{2}>")
    public void testLoadPrivateKey(String keyAlgorithm, String keyFileName, String keyPassword, Class<? extends Exception> exceptionClass, String exceptionMessage) throws Exception {
        if (exceptionClass != null) {
            Throwable exception = assertThrows(exceptionClass, () -> {

                final File keyFile = resourceToFile(keyFileName);
                final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, keyPassword);
                assertThat(privateKey).isNotNull();
            });
            org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString(exceptionMessage));
            return;
        }

        final File keyFile = resourceToFile(keyFileName);
        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, keyPassword);
        assertThat(privateKey).isNotNull();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0} with file <{1}>, password <{2}>")
    public void testCreateNettySslHandler(String keyAlgorithm, String keyFileName, String keyPassword, Class<? extends Exception> exceptionClass, String exceptionMessage) throws Exception {
        if (exceptionClass != null) {
            Throwable exception = assertThrows(exceptionClass, () -> {

                final File keyFile = resourceToFile(keyFileName);
                final File certFile = resourceToFile(CERTIFICATES.get(keyAlgorithm));
                final KeyManager[] keyManagers = KeyUtil.initKeyStore(keyFile, certFile, keyPassword);

                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagers, new TrustManager[0], new SecureRandom());
                assertThat(sslContext.getProtocol()).isEqualTo("TLS");

                final SSLEngine sslEngine = sslContext.createSSLEngine();

                assertThat(sslEngine.getEnabledCipherSuites()).isNotEmpty();
                assertThat(sslEngine.getEnabledProtocols()).isNotEmpty();

                final SslHandler sslHandler = new SslHandler(sslEngine);
                assertThat(sslHandler).isNotNull();
            });
            org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString(exceptionMessage));
            return;
        }

        final File keyFile = resourceToFile(keyFileName);
        final File certFile = resourceToFile(CERTIFICATES.get(keyAlgorithm));
        final KeyManager[] keyManagers = KeyUtil.initKeyStore(keyFile, certFile, keyPassword);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, new TrustManager[0], new SecureRandom());
        assertThat(sslContext.getProtocol()).isEqualTo("TLS");

        final SSLEngine sslEngine = sslContext.createSSLEngine();

        assertThat(sslEngine.getEnabledCipherSuites()).isNotEmpty();
        assertThat(sslEngine.getEnabledProtocols()).isNotEmpty();

        final SslHandler sslHandler = new SslHandler(sslEngine);
        assertThat(sslHandler).isNotNull();
    }
}
