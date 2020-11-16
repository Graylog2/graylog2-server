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
package org.graylog2.contentpacks.facades;

import com.google.common.graph.Graph;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.utilities.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UnsupportedEntityFacade implements EntityFacade<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(UnsupportedEntityFacade.class);

    public static final UnsupportedEntityFacade INSTANCE = new UnsupportedEntityFacade();

    @Override
    public NativeEntity<Void> createNativeEntity(Entity entity,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Object> nativeEntities,
                                                 String username) {
        throw new UnsupportedOperationException("Unsupported entity " + entity.toEntityDescriptor());
    }

    @Override
    public Optional<NativeEntity<Void>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return Optional.empty();
    }

    @Override
    public void delete(Void nativeEntity) {
        throw new UnsupportedOperationException("Unsupported entity");
    }

    @Override
    public EntityExcerpt createExcerpt(Void nativeEntity) {
        throw new UnsupportedOperationException("Unsupported entity");
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return Collections.emptySet();
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        LOG.warn("Couldn't collect entity {}", entityDescriptor);
        return Optional.empty();
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        LOG.warn("Couldn't resolve entity {}", entityDescriptor);
        return Graphs.emptyDirectedGraph();
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        LOG.warn("Couldn't resolve entity {}", entity.toEntityDescriptor());
        return Graphs.emptyDirectedGraph();
    }
}
