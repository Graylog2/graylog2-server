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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jakarta.inject.Inject;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.entitygroups.contentpacks.entities.EntityGroupEntity;
import org.graylog2.entitygroups.model.DBEntityGroupService;
import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.entitygroups.model.Groupable;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// TODO: Implement methods
public class EntityGroupFacade implements EntityFacade<EntityGroup> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityGroupFacade.class);

    public static final ModelType TYPE_V1 = ModelType.of("entity_group", "1");

    private final ObjectMapper objectMapper;
    private final DBEntityGroupService dbEntityGroupService;
    private final Map<String, Groupable> groupableEntityTypes;
    private final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades;

    @Inject
    public EntityGroupFacade(ObjectMapper objectMapper,
                             DBEntityGroupService dbEntityGroupService,
                             Map<String, Groupable> groupableEntityTypes,
                             Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades) {
        this.objectMapper = objectMapper;
        this.dbEntityGroupService = dbEntityGroupService;
        this.groupableEntityTypes = groupableEntityTypes;
        this.entityFacades = entityFacades;
    }

    public Entity exportNativeEntity(EntityGroup entityGroup, EntityDescriptorIds entityDescriptorIds) {
        final Set<EntityDescriptor> descriptorIds = new HashSet<>();

        if (entityGroup.entities() != null) {
            for (Map.Entry<String, Set<String>> typeGroup : entityGroup.entities().entrySet()) {
                final ModelType modelType = groupableEntityTypes.get(typeGroup.getKey()).modelType();
                for (String nativeEntityId : typeGroup.getValue()) {
                    entityDescriptorIds.get(EntityDescriptor.create(nativeEntityId, modelType))
                            .orElseThrow(() -> new ContentPackException("Could not find descriptor for entity " + nativeEntityId));
                    descriptorIds.add(EntityDescriptor.create(nativeEntityId, modelType));
                }
            }
        }

        final EntityGroupEntity contentPackEntity = EntityGroupEntity.builder()
                .name(entityGroup.name())
                .entities(descriptorIds)
                .build();

        final JsonNode data = objectMapper.convertValue(contentPackEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(entityGroup.id(), ModelTypes.ENTITY_GROUP)))
                .type(ModelTypes.ENTITY_GROUP)
                //TODO: make sure this is the right version
                .constraints(ImmutableSet.of(GraylogVersionConstraint.of(Version.from(6, 2, 1))))
                .data(data)
                .build();
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        final Optional<EntityGroup> entityGroup = dbEntityGroupService.get(modelId.id());
        if (entityGroup.isEmpty()) {
            LOG.debug("Couldn't find entity group {}", entityDescriptor);
            return Optional.empty();
        }
        return Optional.of(exportNativeEntity(entityGroup.get(), entityDescriptorIds));
    }

    @Override
    public NativeEntity<EntityGroup> createNativeEntity(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities, String username) throws InvalidRangeParametersException {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<EntityGroup> decode(EntityV1 entity,
                                                    Map<String, ValueReference> parameters,
                                                    Map<EntityDescriptor, Object> nativeEntities) {
        final EntityGroupEntity entityGroupEntity = objectMapper.convertValue(entity.data(), EntityGroupEntity.class);

        final Multimap<String, String> entities = MultimapBuilder.hashKeys().hashSetValues().build();
        for (EntityDescriptor descriptor : entityGroupEntity.entities()) {
            final Object contentPackEntity = nativeEntities.get(descriptor);
            if (contentPackEntity instanceof Groupable groupableEntity) {
                entities.put(groupableEntity.entityTypeName(), groupableEntity.entityId());
            }
        }
        final EntityGroup savedGroup = dbEntityGroupService.save(entityGroupEntity.toBuilder().entities(entities.asMap()));
        return NativeEntity.create(entity.id(), savedGroup.id(), ModelTypes.EVENT_DEFINITION_V1, savedGroup.name(), savedGroup);
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
