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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.opamp.OpAmpCaService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link CollectorsConfigResource} using real MongoDB-backed services.
 * <p>
 * Tests the full PUT lifecycle: create inputs, keep existing, port change restart, delete inputs.
 * OpAmpCaService is mocked since CA hierarchy creation has its own integration tests.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class CollectorsConfigResourceIT {

    @Mock
    private OpAmpCaService opAmpCaService;
    @Mock
    private ExtractorFactory extractorFactory;
    @Mock
    private ConverterFactory converterFactory;
    @Mock
    private MessageInputFactory messageInputFactory;

    private ClusterConfigService clusterConfigService;
    private InputServiceImpl inputService;
    private CollectorsConfigResource resource;

    @BeforeEach
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    void setUp(MongoCollections mongoCollections) {
        final var objectMapper = new ObjectMapperProvider().get();
        final var mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final var clusterEventBus = new ClusterEventBus("collectors-test", Executors.newSingleThreadExecutor());

        clusterConfigService = new ClusterConfigServiceImpl(
                mapperProvider,
                mongoCollections.connection(),
                new SimpleNodeId("test-node"),
                new RestrictedChainingClassLoader(
                        new ChainingClassLoader(getClass().getClassLoader()),
                        SafeClasses.allGraylogInternal()),
                clusterEventBus
        );

        inputService = new InputServiceImpl(
                mongoCollections,
                extractorFactory,
                converterFactory,
                messageInputFactory,
                clusterEventBus,
                objectMapper);

        final var httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com:443/"));

        when(opAmpCaService.getOpAmpCaId()).thenReturn("ca-id");
        when(opAmpCaService.getTokenSigningCertId()).thenReturn("token-id");
        when(opAmpCaService.getOtlpServerCertId()).thenReturn("otlp-id");

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
    void getReturnsDefaultsWhenNoConfigExists() {
        final var requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());

        final var config = resource.get(requestContext);

        assertThat(config.opampCaId()).isNull();
        assertThat(config.http().enabled()).isTrue();
        assertThat(config.http().hostname()).isEqualTo("graylog.example.com");
        assertThat(config.http().port()).isEqualTo(CollectorsConfigResource.DEFAULT_HTTP_PORT);
        assertThat(config.http().inputId()).isNull();
        assertThat(config.grpc().enabled()).isFalse();
    }

    @Test
    void fullLifecycleCreateAndDelete() throws NotFoundException {
        // 1. PUT with HTTP enabled → CA initialized, input created, config persisted
        final var createRequest = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var created = resource.put(createRequest);

        assertThat(created.opampCaId()).isEqualTo("ca-id");
        assertThat(created.tokenSigningCertId()).isEqualTo("token-id");
        assertThat(created.otlpServerCertId()).isEqualTo("otlp-id");
        assertThat(created.http().inputId()).isNotNull();
        assertThat(created.http().enabled()).isTrue();
        assertThat(created.grpc().inputId()).isNull();
        assertThat(created.grpc().enabled()).isFalse();

        // 2. Verify input exists in DB
        final var httpInput = inputService.find(created.http().inputId());
        assertThat(httpInput.getTitle()).isEqualTo("Collector Ingest (HTTP)");
        assertThat(httpInput.isGlobal()).isTrue();

        // 3. Verify config persisted to ClusterConfigService
        final var persisted = clusterConfigService.get(CollectorsConfig.class);
        assertThat(persisted).isNotNull();
        assertThat(persisted.http().inputId()).isEqualTo(created.http().inputId());

        // 4. PUT with HTTP disabled → input deleted
        final var deleteRequest = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var deleted = resource.put(deleteRequest);

        assertThat(deleted.http().inputId()).isNull();

        // 5. Verify input is gone from DB
        final var allInputs = inputService.all();
        assertThat(allInputs).isEmpty();
    }

    @Test
    void putKeepsExistingInputOnIdempotentCall() throws NotFoundException {
        // 1. Create initial config with HTTP enabled
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var first = resource.put(request);
        final String firstInputId = first.http().inputId();
        assertThat(firstInputId).isNotNull();

        // 2. PUT again with same settings → same input kept
        final var second = resource.put(request);

        assertThat(second.http().inputId()).isEqualTo(firstInputId);

        // 3. Only one input in DB
        assertThat(inputService.all()).hasSize(1);
    }

    @Test
    void putCreatesNewInputWhenStaleReferenceExists() {
        // 1. Write a config with a stale input ID that doesn't exist in DB
        final var staleConfig = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "host", 14401, "nonexistent-input-id"),
                new IngestEndpointConfig(false, "host", 14402, null)
        );
        clusterConfigService.write(staleConfig);

        // 2. PUT with HTTP enabled → should create new input since stale one is gone
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14402)
        );

        final var result = resource.put(request);

        assertThat(result.http().inputId()).isNotNull();
        assertThat(result.http().inputId()).isNotEqualTo("nonexistent-input-id");
        assertThat(inputService.all()).hasSize(1);
    }

    @Test
    void putEnablesBothProtocols() throws NotFoundException {
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14402)
        );

        final var result = resource.put(request);

        assertThat(result.http().inputId()).isNotNull();
        assertThat(result.grpc().inputId()).isNotNull();
        assertThat(result.http().inputId()).isNotEqualTo(result.grpc().inputId());

        assertThat(inputService.all()).hasSize(2);

        final var httpInput = inputService.find(result.http().inputId());
        assertThat(httpInput.getTitle()).isEqualTo("Collector Ingest (HTTP)");

        final var grpcInput = inputService.find(result.grpc().inputId());
        assertThat(grpcInput.getTitle()).isEqualTo("Collector Ingest (gRPC)");
    }
}
