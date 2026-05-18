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
package org.graylog2.metrics;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.metrics.cache.EntityMetricsDescriptor;

/**
 * Guice module for entity metrics bindings.
 * <p>
 * Registers open-source {@link EntityMetricsDescriptor} implementations via named multibindings.
 * Enterprise plugins add additional descriptors to the same named sets in their own modules.
 * </p>
 * <p>
 * Entity type constants correspond to MongoDB collection names (e.g. "inputs", "streams"),
 * aligning with {@code @DbEntity} registrations and the catalog title resolution API.
 * </p>
 */
public class EntityMetricsModule extends AbstractModule {

    public static final String ENTITY_TYPE_INPUTS = InputServiceImpl.COLLECTION_NAME;

    @Override
    protected void configure() {
        final Multibinder<EntityMetricsDescriptor> inputDescriptors =
                Multibinder.newSetBinder(binder(), EntityMetricsDescriptor.class, Names.named(ENTITY_TYPE_INPUTS));
        inputDescriptors.addBinding().to(InputMessageCountDescriptor.class);
        inputDescriptors.addBinding().to(InputAssociatedStreamsDescriptor.class);
    }
}
