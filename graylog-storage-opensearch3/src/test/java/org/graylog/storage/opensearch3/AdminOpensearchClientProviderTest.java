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
import org.graylog.security.certutil.CaKeystoreException;
import org.graylog.security.certutil.ClientCertSslContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.transport.OpenSearchTransport;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOpensearchClientProviderTest {

    private ClientCertSslContextFactory sslContextFactory;
    private OfficialOpensearchClientProvider transportProvider;
    private List<URI> hosts;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.sslContextFactory = mock(ClientCertSslContextFactory.class);
        when(sslContextFactory.buildClientCertSslContext(any(), any()))
                .thenAnswer(inv -> SSLContext.getInstance("TLS"));

        this.transportProvider = mock(OfficialOpensearchClientProvider.class);
        when(transportProvider.buildTransport(any(), any())).thenAnswer(inv -> mock(OpenSearchTransport.class));

        this.hosts = List.of(URI.create("http://localhost:9200"));
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void propagatesNoCaError() {
        when(sslContextFactory.buildClientCertSslContext(any(), any()))
                .thenThrow(new CaKeystoreException("Cannot mint client certificate: no CA configured."));

        final AdminOpensearchClientProvider provider = newProvider(Clock.systemUTC());

        assertThatThrownBy(provider::getAdminClient)
                .isInstanceOf(CaKeystoreException.class)
                .hasMessageContaining("no CA configured");
    }

    @Test
    void cachesClientAcrossCalls() {
        final AdminOpensearchClientProvider provider = newProvider(Clock.systemUTC());

        final OfficialOpensearchClient first = provider.getAdminClient();
        final OfficialOpensearchClient second = provider.getAdminClient();

        assertThat(second).isSameAs(first);
        verify(sslContextFactory, times(1)).buildClientCertSslContext(any(), any());
        verify(transportProvider, times(1)).buildTransport(any(), any());
    }

    @Test
    void refreshesTransportWhenCertNearsExpiryButKeepsClientReference() {
        final Instant start = Instant.parse("2026-01-01T00:00:00Z");
        final MutableClock clock = new MutableClock(start);
        final AdminOpensearchClientProvider provider = newProvider(clock);

        final OfficialOpensearchClient initialClient = provider.getAdminClient();

        // Jump past the refresh window (cert lifetime minus the 1-minute refresh buffer).
        clock.advance(AdminOpensearchClientProvider.CERT_LIFETIME);

        final OfficialOpensearchClient afterRefresh = provider.getAdminClient();

        assertThat(afterRefresh)
                .as("client reference must remain stable across cert rotation")
                .isSameAs(initialClient);
        verify(sslContextFactory, times(2)).buildClientCertSslContext(any(), any());
        verify(transportProvider, times(2)).buildTransport(any(), any());
    }

    @Test
    void requestsCertWithAdminCommonNameAndConfiguredLifetime() {
        newProvider(Clock.systemUTC()).getAdminClient();

        verify(sslContextFactory).buildClientCertSslContext(
                eq("graylog-admin"),
                eq(AdminOpensearchClientProvider.CERT_LIFETIME));
    }

    private AdminOpensearchClientProvider newProvider(Clock clock) {
        return new AdminOpensearchClientProvider(sslContextFactory, hosts, transportProvider, objectMapper, clock);
    }

    /**
     * Mutable test clock — {@link Clock#fixed(Instant, ZoneOffset)} is fixed for the life of
     * the instance, which doesn't work for tests that advance time.
     */
    private static final class MutableClock extends Clock {
        private Instant current;

        MutableClock(Instant initial) {
            this.current = initial;
        }

        void advance(java.time.Duration delta) {
            current = current.plus(delta);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
