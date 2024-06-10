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
package org.graylog2.entitygroups.contentpacks;

import jakarta.inject.Inject;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.facades.UnsupportedEntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.entitygroups.model.DBEntityGroupService;
import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// TODO: Implement methods
public class EntityGroupFacade implements EntityFacade<EntityGroup> {
    public static final ModelType TYPE_V1 = ModelType.of("entity_group", "1");

    private final DBEntityGroupService dbEntityGroupService;
    private final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades;

    @Inject
    public EntityGroupFacade(DBEntityGroupService dbEntityGroupService,
                             Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades) {
        this.dbEntityGroupService = dbEntityGroupService;
        this.entityFacades = entityFacades;
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final EntityGroup entityGroup = dbEntityGroupService.get(entityDescriptor.id().id())
                .orElseThrow(() -> new ContentPackException());

        final Set<EntityDescriptor> entities = new HashSet<>();

        entityGroup.entities().entrySet().forEach(typeGroup -> {
            final EntityWithExcerptFacade facade = entityFacades.getOrDefault(typeGroup.getKey(), UnsupportedEntityFacade.INSTANCE);
            typeGroup.getValue().forEach(entityId -> {
                facade.resolveNativeEntity(EntityDescriptor.create(entityId, typeGroup.getKey()));
            });
        });
    }

    @Override
    public NativeEntity<EntityGroup> createNativeEntity(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities, String username) throws InvalidRangeParametersException {
        return null;
    }

    @Override
    public Optional<NativeEntity<EntityGroup>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return Optional.empty();
    }

    @Override
    public void delete(EntityGroup nativeEntity) {

    }

    @Override
    public EntityExcerpt createExcerpt(EntityGroup nativeEntity) {
        return null;
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return Set.of();
    }
}
