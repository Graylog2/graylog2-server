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
package org.graylog.security.certutil;

import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.security.TrustManagerAndSocketFactoryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientCertSslContextFactoryTest {

    private TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider;
    private KeyPair caKeyPair;
    private CsrSigner csrSigner;

    @BeforeEach
    void setUp() throws Exception {
        this.trustManagerAndSocketFactoryProvider = mock(TrustManagerAndSocketFactoryProvider.class);
        when(trustManagerAndSocketFactoryProvider.getTrustManager()).thenReturn(mock(X509TrustManager.class));

        this.caKeyPair = CertificateGenerator.generate(
                CertRequest.selfSigned("test-ca").isCA(true).validity(Duration.ofDays(1)));
        this.csrSigner = new CsrSigner();
    }

    @Test
    void throwsWhenNoCaConfigured() {
        final CaKeystore caKeystore = mock(CaKeystore.class);
        when(caKeystore.exists()).thenReturn(false);

        final ClientCertSslContextFactory factory = new ClientCertSslContextFactoryImpl(caKeystore, trustManagerAndSocketFactoryProvider);

        Assertions.assertThat(factory.buildClientCertSslContext("anyone", Duration.ofMinutes(1)))
                .isEmpty();
    }

    @Test
    void mintsCertWithRequestedCommonName() {
        final CaKeystore caKeystore = signingCaKeystore();
        final ClientCertSslContextFactory factory = new ClientCertSslContextFactoryImpl(caKeystore, trustManagerAndSocketFactoryProvider);

        factory.buildClientCertSslContext("graylog-admin", Duration.ofMinutes(15));

        final ArgumentCaptor<CertificateSigningRequest> csrCaptor = ArgumentCaptor.forClass(CertificateSigningRequest.class);
        verify(caKeystore).signCertificateRequest(csrCaptor.capture(), any());
        assertThat(csrCaptor.getValue().nodeId()).isEqualTo("graylog-admin");
        assertThat(csrCaptor.getValue().request().getSubject().toString()).isEqualTo("CN=graylog-admin");
    }

    @Test
    void returnsUsableSslContext() {
        final ClientCertSslContextFactory factory = new ClientCertSslContextFactoryImpl(signingCaKeystore(), trustManagerAndSocketFactoryProvider);

        final Optional<SSLContext> sslContext = factory.buildClientCertSslContext("graylog-admin", Duration.ofMinutes(15));

        // Successfully obtaining a socket factory means the underlying KeyManager + TrustManager
        // were initialized correctly (key/cert match, init params valid).
        assertThat(sslContext).hasValueSatisfying(sslC -> Assertions.assertThat(sslC.getSocketFactory()).isNotNull());
        assertThat(sslContext).hasValueSatisfying(sslC -> Assertions.assertThat(sslC.getProtocol()).isEqualTo("TLS"));
    }

    private CaKeystore signingCaKeystore() {
        final CaKeystore caKeystore = mock(CaKeystore.class);
        when(caKeystore.exists()).thenReturn(true);
        when(caKeystore.signCertificateRequest(any(), any())).thenAnswer(inv -> {
            final CertificateSigningRequest req = inv.getArgument(0);
            final Duration lifetime = inv.getArgument(1);
            final X509Certificate signed = csrSigner.sign(
                    caKeyPair.privateKey(), caKeyPair.certificate(), req.request(), lifetime);
            return new CertificateChain(signed, List.of(caKeyPair.certificate()));
        });
        return caKeystore;
    }
}
