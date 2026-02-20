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
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.opamp.OpAmpCaService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorsConfigResourceTest {

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private HttpConfiguration httpConfiguration;
    @Mock
    private InputService inputService;
    @Mock
    private OpAmpCaService opAmpCaService;
    @Mock
    private ContainerRequestContext requestContext;

    private CollectorsConfigResource resource;

    @BeforeEach
    void setUp() {
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com:443/"));
        resource = new CollectorsConfigResource(clusterConfigService, httpConfiguration, inputService, opAmpCaService);

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

        assertThat(result.http().enabled()).isFalse();
        assertThat(result.http().hostname()).isEqualTo("graylog.example.com");
        assertThat(result.http().port()).isEqualTo(14401);
        assertThat(result.http().inputId()).isNull();
        assertThat(result.grpc().enabled()).isFalse();
        assertThat(result.grpc().hostname()).isEqualTo("graylog.example.com");
        assertThat(result.grpc().port()).isEqualTo(14402);
        assertThat(result.grpc().inputId()).isNull();
        assertThat(result.opampCaId()).isNull();
    }

    @Test
    void putInitializesCaHierarchy() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);
        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        resource.put(request);

        verify(opAmpCaService).ensureInitialized();
    }

    @Test
    void putCreatesInputWhenProtocolEnabled() throws ValidationException {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);
        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");
        when(inputService.create(anyMap())).thenReturn(mock(Input.class));
        when(inputService.save(any(Input.class))).thenReturn("new-input-id");

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var result = resource.put(request);

        assertThat(result.http().inputId()).isEqualTo("new-input-id");
        assertThat(result.grpc().inputId()).isNull();
        verify(inputService).save(any(Input.class));
    }

    @Test
    void putDeletesInputWhenProtocolDisabled() throws Exception {
        final var existing = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "host", 14401, "existing-input-id"),
                new IngestEndpointConfig(false, "host", 14402, null)
        );
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);
        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");
        final var mockInput = mock(Input.class);
        when(inputService.find("existing-input-id")).thenReturn(mockInput);

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var result = resource.put(request);

        verify(inputService).destroy(mockInput);
        assertThat(result.http().inputId()).isNull();
    }

    @Test
    void putPersistsConfig() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);
        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        resource.put(request);

        verify(clusterConfigService).write(any(CollectorsConfig.class));
    }

    @Test
    void putHandlesStaleInputReference() throws Exception {
        final var existing = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "host", 14401, "stale-input-id"),
                new IngestEndpointConfig(false, "host", 14402, null)
        );
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);
        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");
        when(inputService.find("stale-input-id")).thenThrow(new NotFoundException("not found"));
        when(inputService.create(anyMap())).thenReturn(mock(Input.class));
        when(inputService.save(any(Input.class))).thenReturn("new-input-id");

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var result = resource.put(request);

        // Should create a new input since the stale one doesn't exist
        verify(inputService).save(any(Input.class));
        assertThat(result.http().inputId()).isEqualTo("new-input-id");
    }

    @Test
    void putKeepsExistingInputWhenNoChange() throws Exception {
        final var existing = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "host", 14401, "existing-input-id"),
                new IngestEndpointConfig(false, "host", 14402, null)
        );
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(existing);
        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");
        final var mockInput = mock(Input.class);
        when(inputService.find("existing-input-id")).thenReturn(mockInput);

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var result = resource.put(request);

        // No new input created, no input deleted
        verify(inputService, never()).save(any(Input.class));
        verify(inputService, never()).destroy(any(Input.class));
        assertThat(result.http().inputId()).isEqualTo("existing-input-id");
    }
}
