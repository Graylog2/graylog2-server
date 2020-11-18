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
package org.graylog2.contentpacks;

import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.plugin.streams.Stream;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * This object maps {@link EntityDescriptor} objects to generated IDs that will be used as ID in
 * {@link org.graylog2.contentpacks.model.entities.Entity} objects.
 */
public class EntityDescriptorIds {
    private final ImmutableMap<EntityDescriptor, String> descriptorIds;

    public static EntityDescriptorIds empty() {
        return new EntityDescriptorIds(ImmutableMap.of());
    }

    public static EntityDescriptorIds of(EntityDescriptor... entityDescriptors) {
        return of(Arrays.asList(entityDescriptors));
    }

    public static EntityDescriptorIds of(Collection<EntityDescriptor> entityDescriptors) {
        final ImmutableMap<EntityDescriptor, String> descriptorIds = entityDescriptors.stream()
                .collect(ImmutableMap.toImmutableMap(Function.identity(), d -> {
                    if (isDefaultStreamDescriptor(d)) {
                        return d.id().id();
                    } else {
                        return UUID.randomUUID().toString();
                    }
                }));

        return new EntityDescriptorIds(descriptorIds);
    }

    public static boolean isDefaultStreamDescriptor(EntityDescriptor descriptor) {
        return ModelTypes.STREAM_V1.equals(descriptor.type()) && Stream.isDefaultStreamId(descriptor.id().id());
    }

    private EntityDescriptorIds(ImmutableMap<EntityDescriptor, String> descriptorIds) {
        this.descriptorIds = descriptorIds;
    }

    public Optional<String> get(final EntityDescriptor descriptor) {
        return Optional.ofNullable(descriptorIds.get(descriptor));
    }

    public Optional<String> get(final String id, final ModelType type) {
        return Optional.ofNullable(descriptorIds.get(EntityDescriptor.create(id, type)));
    }

    public String getOrThrow(EntityDescriptor descriptor) {
        return get(descriptor).orElseThrow(() -> new ContentPackException("Couldn't find entity " + descriptor.toString()));
    }

    public String getOrThrow(final String id, final ModelType type) {
        return get(id, type).orElseThrow(() -> new ContentPackException("Couldn't find entity " + id + "/" + type));
    }
}
