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
package org.graylog.collectors.rest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog.collectors.CollectorInputService;
import org.graylog.collectors.CollectorLogsDestinationService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog.collectors.opamp.OpAmpCaService;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorsConfigResourceTest {

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private CollectorInputService collectorInputService;
    @Mock
    private CollectorLogsDestinationService collectorLogsDestinationService;
    @Mock
    private HttpConfiguration httpConfiguration;
    @Mock
    private OpAmpCaService opAmpCaService;
    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private FleetService fleetService;
    @Mock
    private FleetTransactionLogService fleetTransactionLogService;

    private CollectorsConfigResource resource;

    @BeforeEach
    void setUp() {
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com:443/"));
        resource = new CollectorsConfigResource(
                clusterConfigService,
                collectorInputService,
                collectorLogsDestinationService,
                httpConfiguration,
                fleetService,
                fleetTransactionLogService,
                opAmpCaService
        );

        final var subject = mock(Subject.class);
        lenient().when(subject.getPrincipal()).thenReturn("admin");
        ThreadContext.bind(subject);
    }

    @AfterEach
    void tearDown() {
        ThreadContext.unbindSubject();
    }

    @Test
    void getReturnsExistingConfig() {
        final var existing = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "graylog.example.com", 14401, "input-1"),
                new IngestEndpointConfig(false, "graylog.example.com", 14402, null)
        );
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);

        final var result = resource.get(requestContext);

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void getReturnsDefaultWhenNoConfigExists() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);
        when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());

        final var result = resource.get(requestContext);

        assertThat(result.http().enabled()).isTrue();
        assertThat(result.http().hostname()).isEqualTo("graylog.example.com");
        assertThat(result.http().port()).isEqualTo(14401);
        assertThat(result.http().inputId()).isNull();
        assertThat(result.grpc().enabled()).isFalse();
    }

    @Test
    void putInitializesCaAndDestination() {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        resource.put(request);

        verify(opAmpCaService).ensureInitialized();
        verify(collectorLogsDestinationService).ensureExists();
    }

    @Test
    void putDelegatesInputReconciliation() {
        stubCaService();
        when(collectorInputService.reconcile(any(), isNull(), eq(CollectorIngestHttpInput.class.getCanonicalName()),
                eq(CollectorIngestHttpInput.NAME), anyString())).thenReturn("new-input-id");

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var result = resource.put(request);

        assertThat(result.http().inputId()).isEqualTo("new-input-id");
        assertThat(result.grpc().inputId()).isNull();
    }

    @Test
    void putPersistsConfig() {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        resource.put(request);

        verify(clusterConfigService).write(any(CollectorsConfig.class));
        verify(fleetTransactionLogService).appendFleetMarker(anySet(), eq(MarkerType.INGEST_CONFIG_CHANGED));
    }

    private void stubCaService() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);
        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");
    }
}
