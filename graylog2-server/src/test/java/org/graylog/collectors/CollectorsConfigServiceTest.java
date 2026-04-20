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
package org.graylog.collectors;

import org.graylog.collectors.events.CollectorCaConfigUpdated;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectorsConfigServiceTest {

    private ClusterConfigService clusterConfigService;
    private ClusterEventBus clusterEventBus;
    private CollectorsConfigService service;

    @BeforeEach
    void setUp() {
        clusterConfigService = mock(ClusterConfigService.class);
        clusterEventBus = mock(ClusterEventBus.class);
        final var httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(java.net.URI.create("https://localhost:443/"));
        service = new CollectorsConfigService(clusterConfigService, clusterEventBus, httpConfiguration);
    }

    private CollectorsConfig configWithCerts(String caCertId, String signingCertId, String serverCertId) {
        return CollectorsConfig.createDefaultBuilder("localhost")
                .caCertId(caCertId)
                .signingCertId(signingCertId)
                .otlpServerCertId(serverCertId)
                .build();
    }

    @Test
    void save_firesEventWhenCaCertIdChanges() {
        final var existing = configWithCerts("ca-1", "signing-1", "server-1");
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);

        service.save(configWithCerts("ca-2", "signing-1", "server-1"));

        verify(clusterEventBus).post(any(CollectorCaConfigUpdated.class));
    }

    @Test
    void save_firesEventWhenSigningCertIdChanges() {
        final var existing = configWithCerts("ca-1", "signing-1", "server-1");
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);

        service.save(configWithCerts("ca-1", "signing-2", "server-1"));

        verify(clusterEventBus).post(any(CollectorCaConfigUpdated.class));
    }

    @Test
    void save_firesEventWhenServerCertIdChanges() {
        final var existing = configWithCerts("ca-1", "signing-1", "server-1");
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);

        service.save(configWithCerts("ca-1", "signing-1", "server-2"));

        verify(clusterEventBus).post(any(CollectorCaConfigUpdated.class));
    }

    @Test
    void save_doesNotFireEventWhenCertIdsUnchanged() {
        final var existing = configWithCerts("ca-1", "signing-1", "server-1");
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);

        service.save(configWithCerts("ca-1", "signing-1", "server-1"));

        verify(clusterEventBus, never()).post(any());
    }

    @Test
    void save_doesNotFireEventWhenNonCertFieldChanges() {
        final var existing = configWithCerts("ca-1", "signing-1", "server-1");
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);

        // Change only the HTTP port, not cert IDs
        final var updated = existing.toBuilder()
                .http(new IngestEndpointConfig("localhost", 9999))
                .build();
        service.save(updated);

        verify(clusterEventBus, never()).post(any());
    }

    @Test
    void save_doesNotFireEventOnFirstSave() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        service.save(configWithCerts("ca-1", "signing-1", "server-1"));

        verify(clusterEventBus, never()).post(any());
    }
}
