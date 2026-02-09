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
package org.graylog.security.pki;

import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that Ed25519 TLS server certs work with {@link SslProvider#JDK} but not with
 * {@link SslProvider#OPENSSL} (netty-tcnative/BoringSSL).
 * <p>
 * The issue is that netty-tcnative's JNI bridge cannot load Ed25519 private keys into the native
 * BoringSSL SSL context. BoringSSL itself supports Ed25519 TLS signatures, but the Java-to-native
 * key conversion doesn't handle Ed25519 key material. See
 * <a href="https://github.com/netty/netty/issues/10916">Netty #10916</a>.
 * <p>
 * The OpAMP OTLP transports rely on {@link SslProvider#JDK} because of this limitation. If
 * {@code openSslProviderRejectsEd25519ServerCert} starts passing in a future netty-tcnative version,
 * the Netty issue has been fixed and the OpAMP transports could switch back to the default SSL provider.
 * If {@code jdkProviderSupportsEd25519ServerCert} starts failing, something has regressed in the JDK
 * provider and the OpAMP mTLS setup needs investigation.
 */
class Ed25519SslProviderTest {

    private EncryptedValueService encryptedValueService;
    private CertificateBuilder builder;

    @BeforeAll
    static void setupProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @BeforeEach
    void setUp() {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(encryptedValueService, "Test");
    }

    @Test
    void openSslIsAvailable() {
        // Confirm netty-tcnative/BoringSSL is on the classpath — otherwise this test is meaningless.
        assertThat(OpenSsl.isAvailable())
                .as("netty-tcnative (BoringSSL) should be available on the classpath")
                .isTrue();
    }

    @Test
    void jdkProviderSupportsEd25519ServerCert() throws Exception {
        final var keyAndCert = generateEd25519SelfSignedCert();

        assertThatCode(() ->
                SslContextBuilder.forServer(keyAndCert.privateKey(), keyAndCert.cert())
                        .sslProvider(SslProvider.JDK)
                        .build()
        ).doesNotThrowAnyException();
    }

    @Test
    void openSslProviderRejectsEd25519ServerCert() throws Exception {
        final var keyAndCert = generateEd25519SelfSignedCert();

        assertThatThrownBy(() ->
                SslContextBuilder.forServer(keyAndCert.privateKey(), keyAndCert.cert())
                        .sslProvider(SslProvider.OPENSSL)
                        .build()
        ).isInstanceOf(Exception.class);
    }

    @Test
    void openSslProviderSupportsRsaServerCert() throws Exception {
        // Sanity check: RSA works fine with both providers.
        final var keyAndCert = generateRsaSelfSignedCert();

        assertThatCode(() ->
                SslContextBuilder.forServer(keyAndCert.privateKey(), keyAndCert.cert())
                        .sslProvider(SslProvider.OPENSSL)
                        .build()
        ).doesNotThrowAnyException();

        assertThatCode(() ->
                SslContextBuilder.forServer(keyAndCert.privateKey(), keyAndCert.cert())
                        .sslProvider(SslProvider.JDK)
                        .build()
        ).doesNotThrowAnyException();
    }

    private record KeyAndCert(PrivateKey privateKey, X509Certificate cert) {}

    private KeyAndCert generateEd25519SelfSignedCert() throws Exception {
        final var entry = builder.createRootCa("Test Ed25519 CA", Algorithm.ED25519, Duration.ofDays(1));
        final var cert = PemUtils.parseCertificate(entry.certificate());
        final var privateKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(entry.privateKey()));
        return new KeyAndCert(privateKey, cert);
    }

    private KeyAndCert generateRsaSelfSignedCert() throws Exception {
        final var entry = builder.createRootCa("Test RSA CA", Algorithm.RSA_4096, Duration.ofDays(1));
        final var cert = PemUtils.parseCertificate(entry.certificate());
        final var privateKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(entry.privateKey()));
        return new KeyAndCert(privateKey, cert);
    }
}
