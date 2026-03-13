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

import org.graylog.collectors.rest.CollectorsConfigRequest;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class CollectorInputServiceIT {

    @Mock
    private ExtractorFactory extractorFactory;
    @Mock
    private ConverterFactory converterFactory;
    @Mock
    private MessageInputFactory messageInputFactory;

    private InputServiceImpl inputService;
    private CollectorInputService collectorInputService;

    @BeforeEach
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    void setUp(MongoCollections mongoCollections) {
        final var objectMapper = new ObjectMapperProvider().get();
        final var clusterEventBus = new ClusterEventBus("collectors-test", Executors.newSingleThreadExecutor());

        inputService = new InputServiceImpl(
                mongoCollections,
                extractorFactory,
                converterFactory,
                messageInputFactory,
                clusterEventBus,
                objectMapper);

        collectorInputService = new CollectorInputService(inputService);
    }

    @Test
    void fullLifecycleCreateAndDelete() throws NotFoundException {
        final var createRequest = new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401);

        // 1. Create input
        final String inputId = collectorInputService.reconcile(
                createRequest, null,
                "org.graylog.collectors.input.CollectorIngestHttpInput",
                "Collector Ingest (HTTP)", "admin");

        assertThat(inputId).isNotNull();
        final var httpInput = inputService.find(inputId);
        assertThat(httpInput.getTitle()).isEqualTo("Collector Ingest (HTTP)");
        assertThat(httpInput.isGlobal()).isTrue();

        // 2. Delete input
        final var existing = new IngestEndpointConfig(true, "host", 14401, inputId);
        final var disableRequest = new CollectorsConfigRequest.IngestEndpointRequest(false, "host", 14401);

        final String deletedId = collectorInputService.reconcile(
                disableRequest, existing,
                "org.graylog.collectors.input.CollectorIngestHttpInput",
                "Collector Ingest (HTTP)", "admin");

        assertThat(deletedId).isNull();
        assertThat(inputService.all()).isEmpty();
    }

    @Test
    void reconcileKeepsExistingInput() throws NotFoundException {
        final var request = new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401);

        // 1. Create
        final String firstId = collectorInputService.reconcile(
                request, null,
                "org.graylog.collectors.input.CollectorIngestHttpInput",
                "Collector Ingest (HTTP)", "admin");

        // 2. Reconcile again with same settings
        final var existing = new IngestEndpointConfig(true, "host", 14401, firstId);
        final String secondId = collectorInputService.reconcile(
                request, existing,
                "org.graylog.collectors.input.CollectorIngestHttpInput",
                "Collector Ingest (HTTP)", "admin");

        assertThat(secondId).isEqualTo(firstId);
        assertThat(inputService.all()).hasSize(1);
    }

    @Test
    void reconcileCreatesNewInputWhenStaleReference() {
        final var existing = new IngestEndpointConfig(true, "host", 14401, "nonexistent-input-id");
        final var request = new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401);

        final String newId = collectorInputService.reconcile(
                request, existing,
                "org.graylog.collectors.input.CollectorIngestHttpInput",
                "Collector Ingest (HTTP)", "admin");

        assertThat(newId).isNotNull();
        assertThat(newId).isNotEqualTo("nonexistent-input-id");
        assertThat(inputService.all()).hasSize(1);
    }

    @Test
    void reconcileCreatesBothProtocols() {
        final String httpId = collectorInputService.reconcile(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14401),
                null,
                "org.graylog.collectors.input.CollectorIngestHttpInput",
                "Collector Ingest (HTTP)", "admin");

        final String grpcId = collectorInputService.reconcile(
                new CollectorsConfigRequest.IngestEndpointRequest(true, "host", 14402),
                null,
                "org.graylog.collectors.input.CollectorIngestGrpcInput",
                "Collector Ingest (gRPC)", "admin");

        assertThat(httpId).isNotNull();
        assertThat(grpcId).isNotNull();
        assertThat(httpId).isNotEqualTo(grpcId);
        assertThat(inputService.all()).hasSize(2);
    }
}
