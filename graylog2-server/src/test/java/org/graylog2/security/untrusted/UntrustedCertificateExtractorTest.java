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
package org.graylog2.security.untrusted;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import jakarta.annotation.Nonnull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

class UntrustedCertificateExtractorTest {

    private HttpsServer httpsServer;

    @BeforeEach
    void setUp() throws Exception {
        final KeyPair keyPair = CertificateGenerator.generate(CertRequest.selfSigned("junit-extractor-test").validity(Duration.ofDays(1)));
        final char[] password = "my-password".toCharArray();
        final KeyStore keystore = keyPair.toKeystore("ca", password);

        InetSocketAddress address = new InetSocketAddress(0); // means find any free port

        httpsServer = HttpsServer.create(address, 0);
        SSLContext sslContext = SSLContext.getInstance("TLS");

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        tmf.init(keystore);

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                try {
                    SSLContext context = getSSLContext();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    SSLParameters sslParameters = context.getSupportedSSLParameters();
                    params.setSSLParameters(sslParameters);

                } catch (Exception ex) {
                    System.out.println("Failed to create HTTPS port");
                }
            }
        });
        httpsServer.createContext("/", exchange -> {
            String response = "This is the response";
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        });
        httpsServer.setExecutor(null); // creates default executor
        httpsServer.start();
    }

    @AfterEach
    void tearDown() {
        httpsServer.stop(1);
    }

    @Test
    void testExtraction() throws NoSuchAlgorithmException, IOException, KeyManagementException, KeyStoreException, CertificateException {
        final String host = "https://localhost:" + httpsServer.getAddress().getPort();

        final OkHttpClient client = new OkHttpClient.Builder().build();
        final Request request = new Request.Builder().get().url(host).build();

        // firsr verify that the call to the self-signed server fails
        Assertions.assertThatThrownBy(() -> {
            try (final Response ignored = client.newCall(request).execute()) {
            }
        }).isInstanceOf(SSLHandshakeException.class);

        // extract server certificates
        final UntrustedCertificateExtractor extractor = new UntrustedCertificateExtractor(client);
        final List<X509Certificate> certificates = extractor.extractUntrustedCerts(host);
        // and add them to the truststore configured for this client
        final OkHttpClient clientWithTrust = clientWithTruststore(client, certificates);

        // now verify the connection again, should work without errors and deliver response
        try (final Response response = clientWithTrust.newCall(request).execute()) {
            Assertions.assertThat(response.body()).isNotNull();
            Assertions.assertThat(response.body().string()).isEqualTo("This is the response");
        }

    }

    @Nonnull
    private static OkHttpClient clientWithTruststore(OkHttpClient client, List<X509Certificate> certificates) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        KeyStore trustStore = KeyStore.getInstance(CertConstants.PKCS12);
        trustStore.load(null, null);
        certificates.forEach(c -> {
            try {
                trustStore.setCertificateEntry(c.getSubjectX500Principal().getName(), c);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        });

        SSLContext sslContext = SSLContext.getInstance("SSL");
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

        final X509TrustManager trustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        return client.newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                .hostnameVerifier(new NoopHostnameVerifier()) // let's ignore the hostnames for now, not needed to verify certs
                .build();
    }
}
