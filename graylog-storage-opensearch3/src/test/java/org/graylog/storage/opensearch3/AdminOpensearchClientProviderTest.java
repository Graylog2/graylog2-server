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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.security.certutil.CaKeystore;
import org.graylog.security.certutil.CaKeystoreException;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.indexer.security.IndexerAdminCertConstants;
import org.graylog2.security.TrustManagerAndSocketFactoryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.transport.OpenSearchTransport;

import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOpensearchClientProviderTest {

    private TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider;
    private OfficialOpensearchClientProvider transportProvider;
    private List<URI> hosts;
    private ObjectMapper objectMapper;
    private KeyPair caKeyPair;
    private CsrSigner csrSigner;

    @BeforeEach
    void setUp() throws Exception {
        this.trustManagerAndSocketFactoryProvider = mock(TrustManagerAndSocketFactoryProvider.class);
        when(trustManagerAndSocketFactoryProvider.getTrustManager()).thenReturn(mock(X509TrustManager.class));

        this.transportProvider = mock(OfficialOpensearchClientProvider.class);
        when(transportProvider.buildTransport(any(), any())).thenAnswer(inv -> mock(OpenSearchTransport.class));

        this.hosts = List.of(URI.create("http://localhost:9200"));
        this.objectMapper = new ObjectMapper();

        this.caKeyPair = CertificateGenerator.generate(
                CertRequest.selfSigned("test-ca").isCA(true).validity(Duration.ofDays(1)));
        this.csrSigner = new CsrSigner();
    }

    @Test
    void throwsWhenNoCaConfigured() {
        final CaKeystore caKeystore = mock(CaKeystore.class);
        when(caKeystore.exists()).thenReturn(false);

        final AdminOpensearchClientProvider provider = new AdminOpensearchClientProvider(
                caKeystore, hosts, trustManagerAndSocketFactoryProvider, transportProvider, objectMapper);

        assertThatThrownBy(provider::getAdminClient)
                .isInstanceOf(CaKeystoreException.class)
                .hasMessageContaining("no CA configured");
    }

    @Test
    void cachesClientAcrossCalls() {
        final AdminOpensearchClientProvider provider = newProviderWithSigningCa();

        final OfficialOpensearchClient first = provider.getAdminClient();
        final OfficialOpensearchClient second = provider.getAdminClient();

        assertThat(second).isSameAs(first);
        verify(transportProvider, times(1)).buildTransport(any(), any());
    }

    @Test
    void refreshesTransportWhenCertNearsExpiryButKeepsClientReference() {
        final AtomicReference<Instant> clock = new AtomicReference<>(Instant.now());
        final AdminOpensearchClientProvider provider = newProviderWithSigningCa(clock::get);

        final OfficialOpensearchClient initialClient = provider.getAdminClient();

        // Jump past the refresh window (cert lifetime minus the 1-minute refresh buffer).
        clock.set(clock.get().plus(AdminOpensearchClientProvider.CERT_LIFETIME));

        final OfficialOpensearchClient afterRefresh = provider.getAdminClient();

        assertThat(afterRefresh)
                .as("client reference must remain stable across cert rotation")
                .isSameAs(initialClient);
        verify(transportProvider, times(2)).buildTransport(any(), any());
    }

    @Test
    void mintsCertWithAdminCommonName() {
        final CaKeystore caKeystore = signingCaKeystore();
        final AdminOpensearchClientProvider provider = new AdminOpensearchClientProvider(
                caKeystore, hosts, trustManagerAndSocketFactoryProvider, transportProvider, objectMapper);

        provider.getAdminClient();

        final ArgumentCaptor<CertificateSigningRequest> csrCaptor = ArgumentCaptor.forClass(CertificateSigningRequest.class);
        verify(caKeystore).signCertificateRequest(csrCaptor.capture(), any());
        assertThat(csrCaptor.getValue().nodeId()).isEqualTo(IndexerAdminCertConstants.ADMIN_CN);
        assertThat(csrCaptor.getValue().request().getSubject().toString())
                .isEqualTo("CN=" + IndexerAdminCertConstants.ADMIN_CN);
    }

    private AdminOpensearchClientProvider newProviderWithSigningCa() {
        return newProviderWithSigningCa(Instant::now);
    }

    private AdminOpensearchClientProvider newProviderWithSigningCa(Supplier<Instant> clock) {
        final CaKeystore caKeystore = signingCaKeystore();
        return new AdminOpensearchClientProvider(
                caKeystore, hosts, trustManagerAndSocketFactoryProvider, transportProvider, objectMapper) {
            @Override
            protected Instant now() {
                return clock.get();
            }
        };
    }

    /**
     * Builds a mocked {@link CaKeystore} whose {@code signCertificateRequest} actually signs
     * the incoming CSR using an in-test CA, so the resulting cert's public key matches the
     * key pair generated by the provider and {@code SSLContext.init} succeeds.
     */
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
