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
package org.graylog2.shared.bindings.providers;

import com.github.joschi.jadconfig.util.Duration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled("This test is flaky. Maybe a race within MockWebServer")
public class ParameterizedHttpClientProviderTest {

    private final MockWebServer server = new MockWebServer();
    private HandshakeCertificates serverCertificates;
    private HandshakeCertificates clientCertificates;

    @BeforeEach
    void setUp() throws IOException {
        final HeldCertificate localhostCert = new HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .addSubjectAlternativeName("localhost.localdomain")
                .addSubjectAlternativeName("127.0.0.1")
                .build();

        serverCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(localhostCert)
                .build();
        clientCertificates = new HandshakeCertificates.Builder()
                .addTrustedCertificate(localhostCert.certificate())
                .build();

        server.useHttps(serverCertificates.sslSocketFactory(), false);
        server.start();
        server.enqueue(successfulMockResponse());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void tesTlsVerifyAndKeepAlive() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final ParameterizedHttpClientProvider provider = new ParameterizedHttpClientProvider(client(null));
        final OkHttpClient okHttpClient = provider.get(true, false);

        assertThat(okHttpClient.sslSocketFactory().createSocket().getOption(StandardSocketOptions.SO_KEEPALIVE)).isTrue();

        assertThrows(SSLHandshakeException.class,
                () -> okHttpClient.newCall(new Request.Builder().url(server.url("/")).get().build()).execute(),
                "should not have succeeded");
    }

    @Test
    public void testWithSkipTlsVerifyAndKeepAlive() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final ParameterizedHttpClientProvider provider = new ParameterizedHttpClientProvider(client(null));
        final OkHttpClient okHttpClient = provider.get(true, true);

        assertThat(okHttpClient.sslSocketFactory().createSocket().getOption(StandardSocketOptions.SO_KEEPALIVE)).isTrue();

        try (Response response = okHttpClient.newCall(new Request.Builder().url(server.url("/")).get().build()).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }
    }

    @Test
    public void testWithTlsVerifyNoKeepAlive() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final ParameterizedHttpClientProvider provider = new ParameterizedHttpClientProvider(client(null), clientCertificates.sslSocketFactory(), clientCertificates.trustManager());
        final OkHttpClient okHttpClient = provider.get(false, false);
        assertThat(okHttpClient.sslSocketFactory().createSocket().getOption(StandardSocketOptions.SO_KEEPALIVE)).isFalse();

        try (Response response = okHttpClient.newCall(new Request.Builder().url(server.url("/")).get().build()).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }
    }

    @Test
    public void testWithSkipTlsVerifyNoKeepAlive() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final ParameterizedHttpClientProvider provider = new ParameterizedHttpClientProvider(client(null));
        final OkHttpClient okHttpClient = provider.get(false, true);
        assertThat(okHttpClient.sslSocketFactory().createSocket().getOption(StandardSocketOptions.SO_KEEPALIVE)).isFalse();

        try (Response response = okHttpClient.newCall(new Request.Builder().url(server.url("/")).get().build()).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }
    }

    @Test
    @Disabled("Not enabled by default")
    public void testWithSystemDefaultTruststore() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        final ParameterizedHttpClientProvider provider = new ParameterizedHttpClientProvider(client(null));
        final OkHttpClient okHttpClient = provider.get(false, false);

        try (Response response = okHttpClient.newCall(new Request.Builder().url("https://google.com").get().build()).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }
    }

    @Test
    @Disabled("Not enabled by default")
    public void testWithSystemDefaultTruststoreBadHostname() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        final ParameterizedHttpClientProvider provider = new ParameterizedHttpClientProvider(client(null));
        final OkHttpClient okHttpClient = provider.get(true, true);

        try (Response response = okHttpClient.newCall(new Request.Builder().url("https://wrong.host.badssl.com").get().build()).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }
    }

    private OkHttpClientProvider client(URI proxyURI) {
        final OkHttpClientProvider provider = new OkHttpClientProvider(
                Duration.milliseconds(500L),
                Duration.milliseconds(500L),
                Duration.milliseconds(500L),
                proxyURI,
                null, null);

        return provider;
    }

    private MockResponse successfulMockResponse() {
        return new MockResponse().setResponseCode(200).setBody("Test");
    }
}
