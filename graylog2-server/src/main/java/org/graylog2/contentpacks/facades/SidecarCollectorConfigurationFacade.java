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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.SidecarCollectorConfigurationEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class SidecarCollectorConfigurationFacade implements EntityFacade<Configuration> {
    private static final Logger LOG = LoggerFactory.getLogger(SidecarCollectorConfigurationFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.SIDECAR_COLLECTOR_CONFIGURATION_V1;

    private final ObjectMapper objectMapper;
    private final ConfigurationService configurationService;

    @Inject
    public SidecarCollectorConfigurationFacade(ObjectMapper objectMapper, ConfigurationService configurationService) {
        this.objectMapper = objectMapper;
        this.configurationService = configurationService;
    }

    @VisibleForTesting
    Entity exportNativeEntity(Configuration configuration, EntityDescriptorIds entityDescriptorIds) {
        final SidecarCollectorConfigurationEntity configurationEntity = SidecarCollectorConfigurationEntity.create(
                ValueReference.of(entityDescriptorIds.getOrThrow(configuration.collectorId(), ModelTypes.SIDECAR_COLLECTOR_V1)),
                ValueReference.of(configuration.name()),
                ValueReference.of(configuration.color()),
                ValueReference.of(configuration.template()));
        final JsonNode data = objectMapper.convertValue(configurationEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(configuration.id(), ModelTypes.SIDECAR_COLLECTOR_CONFIGURATION_V1)))
                .type(TYPE_V1)
                .data(data)
                .build();
    }

    @Override
    public NativeEntity<Configuration> createNativeEntity(Entity entity,
                                                          Map<String, ValueReference> parameters,
                                                          Map<EntityDescriptor, Object> nativeEntities,
                                                          String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<Configuration> decode(EntityV1 entity, Map<String, ValueReference> parameters) {
        final SidecarCollectorConfigurationEntity configurationEntity = objectMapper.convertValue(entity.data(), SidecarCollectorConfigurationEntity.class);
        final Configuration configuration = Configuration.create(
                configurationEntity.collectorId().asString(parameters),
                configurationEntity.title().asString(parameters),
                configurationEntity.color().asString(parameters),
                configurationEntity.template().asString(parameters));

        final Configuration savedConfiguration = configurationService.save(configuration);
        return NativeEntity.create(entity.id(), savedConfiguration.id(), TYPE_V1, configuration.name(), savedConfiguration);
    }

    @Override
    public Optional<NativeEntity<Configuration>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        // TODO: Check if multiple configurations are allowed to exist (bernd)
        return Optional.empty();
    }

    @Override
    public Optional<NativeEntity<Configuration>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return configurationService.get(nativeEntityDescriptor.id().id())
                .map(entity -> NativeEntity.create(nativeEntityDescriptor, entity));
    }

    @Override
    public void delete(Configuration nativeEntity) {
        configurationService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(Configuration configuration) {
        return EntityExcerpt.builder()
                .id(ModelId.of(configuration.id()))
                .type(TYPE_V1)
                .title(configuration.name())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return configurationService.all().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        final Configuration configuration = configurationService.find(modelId.id());
        if (isNull(configuration)) {
            LOG.debug("Couldn't find collector configuration {}", entityDescriptor);
            return Optional.empty();
        }

        return Optional.of(exportNativeEntity(configuration, entityDescriptorIds));
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Configuration configuration = configurationService.find(modelId.id());
        if (isNull(configuration)) {
            LOG.debug("Could not find configuration {}", entityDescriptor);
        } else {
            final EntityDescriptor collectorEntityDescriptor = EntityDescriptor.create(
                    configuration.collectorId(), ModelTypes.SIDECAR_COLLECTOR_V1);
            mutableGraph.putEdge(entityDescriptor, collectorEntityDescriptor);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveEntityV1((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveEntityV1(EntityV1 entity,
                                          Map<String, ValueReference> parameters,
                                          Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final SidecarCollectorConfigurationEntity configurationEntity = objectMapper.convertValue(entity.data(), SidecarCollectorConfigurationEntity.class);
        final EntityDescriptor collectorDescriptor = EntityDescriptor.create(configurationEntity.collectorId().asString(parameters), ModelTypes.SIDECAR_COLLECTOR_V1);
        final Entity collectorEntity = entities.get(collectorDescriptor);

        if (collectorEntity != null) {
            mutableGraph.putEdge(entity, collectorEntity);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
