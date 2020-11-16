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
package org.graylog.events.contentpack.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EventDefinitionFacade implements EntityFacade<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(EventDefinitionFacade.class);

    private final ObjectMapper objectMapper;
    private final EventDefinitionHandler eventDefinitionHandler;
    private final DBJobDefinitionService jobDefinitionService;
    private final DBEventDefinitionService eventDefinitionService;
    private final Set<PluginMetaData> pluginMetaData;
    private final UserService userService;

    @Inject
    public EventDefinitionFacade(ObjectMapper objectMapper,
                                 EventDefinitionHandler eventDefinitionHandler,
                                 Set<PluginMetaData> pluginMetaData,
                                 DBJobDefinitionService jobDefinitionService,
                                 DBEventDefinitionService eventDefinitionService,
                                 UserService userService) {
        this.objectMapper = objectMapper;
        this.pluginMetaData = pluginMetaData;
        this.eventDefinitionHandler = eventDefinitionHandler;
        this.jobDefinitionService = jobDefinitionService;
        this.eventDefinitionService = eventDefinitionService;
        this.userService = userService;
    }

    @VisibleForTesting
    private Entity exportNativeEntity(EventDefinitionDto eventDefinition, EntityDescriptorIds entityDescriptorIds) {
        // Presence of a job definition means that the event definition should be scheduled
        final Optional<JobDefinitionDto> jobDefinition = jobDefinitionService.getByConfigField(EventProcessorExecutionJob.Config.FIELD_EVENT_DEFINITION_ID, eventDefinition.id());

        final EventDefinitionEntity entity = eventDefinition.toContentPackEntity(entityDescriptorIds)
                .toBuilder()
                .isScheduled(ValueReference.of(jobDefinition.isPresent()))
                .build();

        final JsonNode data = objectMapper.convertValue(entity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(eventDefinition.id(), ModelTypes.EVENT_DEFINITION_V1)))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .constraints(versionConstraints(eventDefinition))
                .data(data)
                .build();
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        final Optional<EventDefinitionDto> eventDefinition = eventDefinitionService.get(modelId.id());
        if (!eventDefinition.isPresent()) {
            LOG.debug("Couldn't find event definition {}", entityDescriptor);
            return Optional.empty();
        }
        return Optional.of(exportNativeEntity(eventDefinition.get(), entityDescriptorIds));
    }

    private ImmutableSet<Constraint> versionConstraints(EventDefinitionDto eventDefinitionDto) {
        final String packageName = eventDefinitionDto.config().getContentPackPluginPackage();
        return pluginMetaData.stream()
                .filter(metaData -> packageName.equals(metaData.getClass().getCanonicalName()))
                .map(PluginVersionConstraint::of)
                .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public NativeEntity<EventDefinitionDto> createNativeEntity(Entity entity,
                                                               Map<String, ValueReference> parameters,
                                                               Map<EntityDescriptor, Object> nativeEntities,
                                                               String username) {
        if (entity instanceof EntityV1) {
            final User user = Optional.ofNullable(userService.load(username)).orElseThrow(() -> new IllegalStateException("Cannot load user <" + username + "> from db"));
            return decode((EntityV1) entity, parameters, nativeEntities, user);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<EventDefinitionDto> decode(EntityV1 entity,
                                                    Map<String, ValueReference> parameters,
                                                    Map<EntityDescriptor, Object> nativeEntities, User user) {
        final EventDefinitionEntity eventDefinitionEntity = objectMapper.convertValue(entity.data(),
                EventDefinitionEntity.class);
        final EventDefinitionDto eventDefinition = eventDefinitionEntity.toNativeEntity(parameters, nativeEntities);
        final EventDefinitionDto savedDto;
        if (eventDefinitionEntity.isScheduled().asBoolean(parameters)) {
            savedDto = eventDefinitionHandler.create(eventDefinition, Optional.ofNullable(user));
        } else {
            savedDto = eventDefinitionHandler.createWithoutSchedule(eventDefinition, Optional.ofNullable(user));
        }
        return NativeEntity.create(entity.id(), savedDto.id(), ModelTypes.EVENT_DEFINITION_V1, savedDto.title(), savedDto);
    }

    @Override
    public Optional<NativeEntity<EventDefinitionDto>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        final Optional<EventDefinitionDto> eventDefinition = eventDefinitionService.get(nativeEntityDescriptor.id().id());

        return eventDefinition.map(eventDefinitionDto ->
                NativeEntity.create(nativeEntityDescriptor, eventDefinitionDto));
    }

    @Override
    public void delete(EventDefinitionDto nativeEntity) {
        eventDefinitionHandler.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(EventDefinitionDto nativeEntity) {
        return EntityExcerpt.builder()
                .id(ModelId.of(nativeEntity.id()))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .title(nativeEntity.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return eventDefinitionService.streamAll()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Optional<EventDefinitionDto> eventDefinition = eventDefinitionService.get(modelId.id());
        if (eventDefinition.isPresent()) {
            eventDefinition.get().resolveNativeEntity(entityDescriptor, mutableGraph);
        } else {
            LOG.debug("Couldn't find event definition {}", entityDescriptor);
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

        final EventDefinitionEntity eventDefinition = objectMapper.convertValue(entity.data(), EventDefinitionEntity.class);
        eventDefinition.resolveForInstallation(entity, parameters, entities, graph);

        return ImmutableGraph.copyOf(graph);
    }
}
