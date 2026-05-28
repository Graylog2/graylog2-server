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
package org.graylog2.metrics.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.inputs.metrics.InputExtractorCountDescriptor;
import org.graylog2.inputs.metrics.InputMessagesPerStreamDescriptor;
import org.graylog2.metrics.entity.cache.MetricsCacheService;
import org.graylog2.streams.StreamServiceImpl;
import org.graylog2.streams.metrics.StreamAssociatedInputsDescriptor;
import org.graylog2.streams.metrics.StreamAvgProcessingTimeDescriptor;
import org.graylog2.streams.metrics.StreamMaxProcessingTimeDescriptor;
import org.graylog2.streams.metrics.StreamMessageCountDescriptor;
import org.graylog2.streams.metrics.StreamPipelinesDescriptor;
import org.graylog2.streams.metrics.StreamRoutingPipelinesDescriptor;

import java.util.Set;

/**
 * Guice module for entity metrics bindings.
 * <p>
 * Registers open-source {@link EntityMetricDescriptor} implementations via named multibindings.
 * Enterprise plugins add additional descriptors to the same named sets in their own modules.
 * </p>
 * <p>
 * Entity type constants correspond to MongoDB collection names (e.g. "inputs", "streams"),
 * aligning with {@code @DbEntity} registrations and the catalog title resolution API.
 * </p>
 */
public class EntityMetricsModule extends AbstractModule {

    public static final String ENTITY_TYPE_INPUTS = InputServiceImpl.COLLECTION_NAME;
    public static final String ENTITY_TYPE_STREAMS = StreamServiceImpl.COLLECTION_NAME;

    @Override
    protected void configure() {
        final Multibinder<EntityMetricDescriptor> inputDescriptors =
                Multibinder.newSetBinder(binder(), EntityMetricDescriptor.class, Names.named(ENTITY_TYPE_INPUTS));
        inputDescriptors.addBinding().to(InputMessagesPerStreamDescriptor.class);
        inputDescriptors.addBinding().to(InputExtractorCountDescriptor.class);

        final Multibinder<EntityMetricDescriptor> streamDescriptors =
                Multibinder.newSetBinder(binder(), EntityMetricDescriptor.class, Names.named(ENTITY_TYPE_STREAMS));
        streamDescriptors.addBinding().to(StreamMessageCountDescriptor.class);
        streamDescriptors.addBinding().to(StreamAvgProcessingTimeDescriptor.class);
        streamDescriptors.addBinding().to(StreamMaxProcessingTimeDescriptor.class);
        streamDescriptors.addBinding().to(StreamAssociatedInputsDescriptor.class);
        streamDescriptors.addBinding().to(StreamPipelinesDescriptor.class);
        streamDescriptors.addBinding().to(StreamRoutingPipelinesDescriptor.class);
    }

    @SuppressWarnings("unused")
    @Provides
    @Singleton
    @Named(ENTITY_TYPE_INPUTS)
    EntityMetricsService inputMetricsService(@Named(ENTITY_TYPE_INPUTS) Set<EntityMetricDescriptor> descriptors,
                                             MetricsCacheService cacheService,
                                             ObjectMapper objectMapper) {
        return new EntityMetricsService(ENTITY_TYPE_INPUTS, descriptors, cacheService, objectMapper);
    }

    @SuppressWarnings("unused")
    @Provides
    @Singleton
    @Named(ENTITY_TYPE_STREAMS)
    EntityMetricsService streamMetricsService(@Named(ENTITY_TYPE_STREAMS) Set<EntityMetricDescriptor> descriptors,
                                              MetricsCacheService cacheService,
                                              ObjectMapper objectMapper) {
        return new EntityMetricsService(ENTITY_TYPE_STREAMS, descriptors, cacheService, objectMapper);
    }
}
