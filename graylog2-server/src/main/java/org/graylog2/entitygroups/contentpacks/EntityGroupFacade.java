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
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import jakarta.inject.Inject;
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
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
import org.graylog2.entitygroups.entities.GroupableEntity;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityGroupFacade implements EntityFacade<EntityGroup> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityGroupFacade.class);

    public static final ModelType TYPE_V1 = ModelType.of("entity_group", "1");

    private final ObjectMapper objectMapper;
    private final DBEntityGroupService dbEntityGroupService;
    private final Map<String, GroupableEntity> groupableEntityTypes;

    @Inject
    public EntityGroupFacade(ObjectMapper objectMapper,
                             DBEntityGroupService dbEntityGroupService,
                             Map<String, GroupableEntity> groupableEntityTypes) {
        this.objectMapper = objectMapper;
        this.dbEntityGroupService = dbEntityGroupService;
        this.groupableEntityTypes = groupableEntityTypes;
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

    public Entity exportNativeEntity(EntityGroup entityGroup, EntityDescriptorIds entityDescriptorIds) {
        EntityGroupEntity contentPackEntity = EntityGroupEntity.builder()
                .name(entityGroup.name())
                .entities(Map.of())
                .build();

        for (Map.Entry<String, Set<String>> typeGroup : entityGroup.entities().entrySet()) {
            final ModelType modelType = groupableEntityTypes.get(typeGroup.getKey()).modelType();
            for (String nativeEntityId : typeGroup.getValue()) {
                Optional<String> descriptorId = entityDescriptorIds.get(EntityDescriptor.create(nativeEntityId, modelType));
                if (descriptorId.isPresent()) {
                    contentPackEntity = contentPackEntity.addEntity(typeGroup.getKey(), EntityDescriptor.create(descriptorId.get(), modelType));
                } else {
                    LOG.debug("Couldn't find {} entity with ID {}", typeGroup.getKey(), nativeEntityId);
                }
            }
        }

        final JsonNode data = objectMapper.convertValue(contentPackEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(entityGroup.id(), EntityGroupFacade.TYPE_V1)))
                .type(EntityGroupFacade.TYPE_V1)
                //TODO: make sure this is the right version
                .constraints(ImmutableSet.of(GraylogVersionConstraint.of(Version.from(6, 1, 0))))
                .data(data)
                .build();
    }

    @Override
    public NativeEntity<EntityGroup> createNativeEntity(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities, String username) throws InvalidRangeParametersException {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<EntityGroup> decode(EntityV1 entity,
                                             Map<EntityDescriptor, Object> nativeEntities) {
        final EntityGroupEntity entityGroupEntity = objectMapper.convertValue(entity.data(), EntityGroupEntity.class);
        final EntityGroup nativeGroup = EntityGroup.builder()
                .name(entityGroupEntity.name())
                .entities(Map.of())
                .build();

        for (Map.Entry<String, Set<EntityDescriptor>> typeGroup : entityGroupEntity.entities().entrySet()) {
            for (EntityDescriptor descriptor : typeGroup.getValue()) {
                final ContentPackable<?> nativeEntity = (ContentPackable<?>) nativeEntities.get(descriptor);
                nativeGroup.addEntity(typeGroup.getKey(), nativeEntity.id());
            }
        }

        final EntityGroup savedGroup = dbEntityGroupService.save(nativeGroup);
        return NativeEntity.create(entity.id(), savedGroup.id(), EntityGroupFacade.TYPE_V1, savedGroup.name(), savedGroup);
    }

//    @Override
//    public Optional<NativeEntity<EntityGroup>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
//        if (entity instanceof EntityV1) {
//            return findExisting((EntityV1) entity);
//        } else {
//            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
//        }
//    }
//
//    private Optional<NativeEntity<EntityGroup>> findExisting(EntityV1 entity) {
//
//        final EntityGroupEntity entityGroupEntity = objectMapper.convertValue(entity.data(), EntityGroupEntity.class);
//        group = dbEntityGroupService.getByName(entityGroupEntity.name());
//
//        if (group.isPresent()) {
//            return Optional.of(NativeEntity.create(entity.id(), group.get().id(), EntityGroupFacade.TYPE_V1, group.get().name(), group.get()));
//        }
//        return Optional.empty();
//    }

    @Override
    public Optional<NativeEntity<EntityGroup>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        final Optional<EntityGroup> group = dbEntityGroupService.get(nativeEntityDescriptor.id().id());
        if (group.isPresent()) {
            return Optional.of(NativeEntity.create(nativeEntityDescriptor, group.get()));
        }
        return Optional.empty();
    }

    @Override
    public void delete(EntityGroup nativeEntity) {
        dbEntityGroupService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(EntityGroup nativeEntity) {
        return EntityExcerpt.builder()
                .id(ModelId.of(nativeEntity.id()))
                .type(EntityGroupFacade.TYPE_V1)
                .title(nativeEntity.name())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return dbEntityGroupService.streamAll()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Optional<EntityGroup> entityGroup = dbEntityGroupService.get(modelId.id());
        if (entityGroup.isPresent()) {
            for (Map.Entry<String, Set<String>> typeGroup : entityGroup.get().entities().entrySet()) {
                final GroupableEntity groupable = groupableEntityTypes.get(typeGroup.getKey());
                final ModelType modelType = groupable.modelType();
                for (String nativeEntityId : typeGroup.getValue()) {
                    final EntityDescriptor depEntity = EntityDescriptor.builder()
                            .id(ModelId.of(nativeEntityId))
                            .type(modelType)
                            .build();
                    mutableGraph.putEdge(entityDescriptor, depEntity);
                }
            }
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveForInstallationV1((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveForInstallationV1(EntityV1 entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> graph = GraphBuilder.directed().build();
        graph.addNode(entity);

        final EntityGroupEntity entityGroupEntity = objectMapper.convertValue(entity.data(), EntityGroupEntity.class);
        entityGroupEntity.entities().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(descriptor -> graph.putEdge(entity, descriptor));

        return ImmutableGraph.copyOf(graph);
    }
}
