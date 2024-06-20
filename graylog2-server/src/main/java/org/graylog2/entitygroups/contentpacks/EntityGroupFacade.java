///*
// * Copyright (C) 2020 Graylog, Inc.
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the Server Side Public License, version 1,
// * as published by MongoDB, Inc.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * Server Side Public License for more details.
// *
// * You should have received a copy of the Server Side Public License
// * along with this program. If not, see
// * <http://www.mongodb.com/licensing/server-side-public-license>.
// */
//package org.graylog2.entitygroups.contentpacks;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.collect.ImmutableSet;
//import com.google.common.graph.Graph;
//import com.google.common.graph.GraphBuilder;
//import com.google.common.graph.ImmutableGraph;
//import com.google.common.graph.MutableGraph;
//import com.google.inject.name.Named;
//import jakarta.inject.Inject;
//import org.graylog2.contentpacks.EntityDescriptorIds;
//import org.graylog2.contentpacks.facades.EntityFacade;
//import org.graylog2.contentpacks.model.ModelId;
//import org.graylog2.contentpacks.model.ModelType;
//import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
//import org.graylog2.contentpacks.model.entities.Entity;
//import org.graylog2.contentpacks.model.entities.EntityDescriptor;
//import org.graylog2.contentpacks.model.entities.EntityExcerpt;
//import org.graylog2.contentpacks.model.entities.EntityV1;
//import org.graylog2.contentpacks.model.entities.NativeEntity;
//import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
//import org.graylog2.contentpacks.model.entities.references.ValueReference;
//import org.graylog2.database.MongoEntity;
//import org.graylog2.entitygroups.contentpacks.entities.EntityGroupEntity;
//import org.graylog2.entitygroups.model.DBEntityGroupService;
//import org.graylog2.entitygroups.model.EntityGroup;
//import org.graylog2.plugin.Version;
//import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import static org.graylog2.entitygroups.EntityGroupService.addEntityToMap;
//
//public class EntityGroupFacade implements EntityFacade<EntityGroup> {
//    private static final Logger LOG = LoggerFactory.getLogger(EntityGroupFacade.class);
//
//    public static final ModelType TYPE_V1 = ModelType.of("entity_group", "1");
//
//    private final ObjectMapper objectMapper;
//    private final DBEntityGroupService dbEntityGroupService;
//
//    @Inject
//    public EntityGroupFacade(ObjectMapper objectMapper, DBEntityGroupService dbEntityGroupService) {
//        this.objectMapper = objectMapper;
//        this.dbEntityGroupService = dbEntityGroupService;
//    }
//
//    @Override
//    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
//        final ModelId modelId = entityDescriptor.id();
//        final Optional<EntityGroup> entityGroup = dbEntityGroupService.get(modelId.id());
//        if (entityGroup.isEmpty()) {
//            LOG.debug("Couldn't find entity group {}", entityDescriptor);
//            return Optional.empty();
//        }
//        return Optional.of(exportNativeEntity(entityGroup.get(), entityDescriptorIds));
//    }
//
//    public Entity exportNativeEntity(EntityGroup entityGroup, EntityDescriptorIds entityDescriptorIds) {
//        final Map<String, Set<String>> entities = new HashMap<>();
//
//        for (Map.Entry<String, Set<String>> typeGroup : entityGroup.entities().entrySet()) {
//            final ModelType modelType = TYPE_V1;//groupableEntityHandlers.get(typeGroup.getKey()).modelType();
//            for (String nativeEntityId : typeGroup.getValue()) {
//                Optional<String> descriptorId = entityDescriptorIds.get(EntityDescriptor.create(nativeEntityId, modelType));
//                if (descriptorId.isPresent()) {
//                    addEntityToMap(entities, typeGroup.getKey(), descriptorId.get());
//                } else {
//                    LOG.debug("Couldn't find {} entity with ID {}", typeGroup.getKey(), nativeEntityId);
//                }
//            }
//        }
//
//        final EntityGroupEntity contentPackEntity = entityGroup.toContentPackEntity(entityDescriptorIds)
//                .toBuilder().entities(entities).build();
//        final JsonNode data = objectMapper.convertValue(contentPackEntity, JsonNode.class);
//        return EntityV1.builder()
//                .id(ModelId.of(entityDescriptorIds.getOrThrow(entityGroup.id(), EntityGroupFacade.TYPE_V1)))
//                .type(EntityGroupFacade.TYPE_V1)
//                //TODO: make sure this is the right version
//                .constraints(ImmutableSet.of(GraylogVersionConstraint.of(Version.from(6, 1, 0))))
//                .data(data)
//                .build();
//    }
//
//    @Override
//    public NativeEntity<EntityGroup> createNativeEntity(Entity entity,
//                                                        Map<String, ValueReference> parameters,
//                                                        Map<EntityDescriptor, Object> nativeEntities,
//                                                        String username) throws InvalidRangeParametersException {
//        if (entity instanceof EntityV1) {
//            return decode((EntityV1) entity, parameters, nativeEntities);
//        } else {
//            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
//        }
//    }
//
//    private NativeEntity<EntityGroup> decode(EntityV1 entity,
//                                             Map<String, ValueReference> parameters,
//                                             Map<EntityDescriptor, Object> nativeEntities) {
//        final EntityGroupEntity contentPackGroupEntity = objectMapper.convertValue(entity.data(), EntityGroupEntity.class);
//        final Optional<EntityGroup> existingGroup = dbEntityGroupService.getByName(contentPackGroupEntity.name());
//
//        final Map<String, Set<String>> entities;
//        if (existingGroup.isPresent() && existingGroup.get().entities() != null) {
//            entities = new HashMap<>(existingGroup.get().entities());
//        } else {
//            entities = new HashMap<>();
//        }
//
//        for (Map.Entry<String, Set<String>> typeGroup : contentPackGroupEntity.entities().entrySet()) {
//            //final GroupableEntityHandler entityHandler = groupableEntityHandlers.get(typeGroup.getKey());
//            final ModelType modelType = TYPE_V1;//entityHandler.modelType();
//            for (String entityId : typeGroup.getValue()) {
//                final EntityDescriptor descriptor = EntityDescriptor.create(entityId, modelType);
//                final Object nativeEntity = nativeEntities.get(descriptor);
//                addEntityToMap(entities, typeGroup.getKey(), "");//entityHandler.getEntityId(nativeEntity));
//            }
//        }
//
//        final String groupId = existingGroup.map(MongoEntity::id).orElse(null);
//        final EntityGroup group = contentPackGroupEntity.toNativeEntity(parameters, nativeEntities).toBuilder()
//                .id(groupId)
//                .entities(entities)
//                .build();
//        final EntityGroup savedGroup = dbEntityGroupService.save(group);
//        return NativeEntity.create(entity.id(), savedGroup.id(), EntityGroupFacade.TYPE_V1, savedGroup.name(), savedGroup);
//    }
//
//    @Override
//    public Optional<NativeEntity<EntityGroup>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
//        final Optional<EntityGroup> group = dbEntityGroupService.get(nativeEntityDescriptor.id().id());
//        return group.map(entityGroup -> NativeEntity.create(nativeEntityDescriptor, entityGroup));
//    }
//
//    // TODO: do we want to delete groups when a content pack is uninstalled?
//    // We should probably check if there are any left over entity dependencies first at least.
//    @Override
//    public void delete(EntityGroup nativeEntity) {
//        dbEntityGroupService.delete(nativeEntity.id());
//    }
//
//    @Override
//    public EntityExcerpt createExcerpt(EntityGroup nativeEntity) {
//        return EntityExcerpt.builder()
//                .id(ModelId.of(nativeEntity.id()))
//                .type(EntityGroupFacade.TYPE_V1)
//                .title(nativeEntity.name())
//                .build();
//    }
//
//    @Override
//    public Set<EntityExcerpt> listEntityExcerpts() {
//        try (final Stream<EntityGroup> stream = dbEntityGroupService.streamAll()) {
//            return stream.map(this::createExcerpt)
//                    .collect(Collectors.toSet());
//        }
//    }
//
//    @Override
//    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
//        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
//        mutableGraph.addNode(entityDescriptor);
//
//        final ModelId modelId = entityDescriptor.id();
//        final Optional<EntityGroup> entityGroup = dbEntityGroupService.get(modelId.id());
//        if (entityGroup.isPresent()) {
//            for (Map.Entry<String, Set<String>> typeGroup : entityGroup.get().entities().entrySet()) {
//                //final GroupableEntityHandler entityHandler = groupableEntityHandlers.get(typeGroup.getKey());
//                final ModelType modelType = TYPE_V1;//entityHandler.modelType();
//                for (String nativeEntityId : typeGroup.getValue()) {
//                    final EntityDescriptor depEntity = EntityDescriptor.builder()
//                            .id(ModelId.of(nativeEntityId))
//                            .type(modelType)
//                            .build();
//                    mutableGraph.putEdge(entityDescriptor, depEntity);
//                }
//            }
//        }
//
//        return ImmutableGraph.copyOf(mutableGraph);
//    }
//
//    @Override
//    public Graph<Entity> resolveForInstallation(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Entity> entities) {
//        if (entity instanceof EntityV1) {
//            return resolveForInstallationV1((EntityV1) entity, entities);
//        } else {
//            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
//        }
//    }
//
//    private Graph<Entity> resolveForInstallationV1(EntityV1 entity, Map<EntityDescriptor, Entity> entities) {
//        final MutableGraph<Entity> graph = GraphBuilder.directed().build();
//        graph.addNode(entity);
//
//        final EntityGroupEntity entityGroupEntity = objectMapper.convertValue(entity.data(), EntityGroupEntity.class);
//
//        for (Map.Entry<String, Set<String>> typeGroup : entityGroupEntity.entities().entrySet()) {
//            //final GroupableEntityHandler entityHandler = groupableEntityHandlers.get(typeGroup.getKey());
//            final ModelType modelType = TYPE_V1;//entityHandler.modelType();
//            for (String entityId : typeGroup.getValue()) {
//                graph.putEdge(entity, entities.get(EntityDescriptor.create(entityId, modelType)));
//            }
//        }
//
//        return ImmutableGraph.copyOf(graph);
//    }
//}
