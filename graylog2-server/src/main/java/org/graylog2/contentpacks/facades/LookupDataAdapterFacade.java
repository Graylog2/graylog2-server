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
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.LookupDataAdapterEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toReferenceMap;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toValueMap;

public class LookupDataAdapterFacade implements EntityFacade<DataAdapterDto> {
    public static final ModelType TYPE_V1 = ModelTypes.LOOKUP_ADAPTER_V1;

    private final ObjectMapper objectMapper;
    private final DBDataAdapterService dataAdapterService;
    private final Set<PluginMetaData> pluginMetaData;

    @Inject
    public LookupDataAdapterFacade(ObjectMapper objectMapper,
                                   DBDataAdapterService dataAdapterService,
                                   Set<PluginMetaData> pluginMetaData) {
        this.objectMapper = objectMapper;
        this.dataAdapterService = dataAdapterService;
        this.pluginMetaData = pluginMetaData;
    }

    @VisibleForTesting
    Entity exportNativeEntity(DataAdapterDto dataAdapterDto, EntityDescriptorIds entityDescriptorIds) {
        // TODO: Create independent representation of entity?
        final Map<String, Object> configuration = objectMapper.convertValue(dataAdapterDto.config(), TypeReferences.MAP_STRING_OBJECT);
        final LookupDataAdapterEntity lookupDataAdapterEntity = LookupDataAdapterEntity.create(
                ValueReference.of(dataAdapterDto.name()),
                ValueReference.of(dataAdapterDto.title()),
                ValueReference.of(dataAdapterDto.description()),
                toReferenceMap(configuration));
        final JsonNode data = objectMapper.convertValue(lookupDataAdapterEntity, JsonNode.class);
        final Set<Constraint> constraints = versionConstraints(dataAdapterDto);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(dataAdapterDto.id(), ModelTypes.LOOKUP_ADAPTER_V1)))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .constraints(ImmutableSet.copyOf(constraints))
                .data(data)
                .build();
    }


    private Set<Constraint> versionConstraints(DataAdapterDto dataAdapterDto) {
        // TODO: Find more robust method of identifying the providing plugin
        final String packageName = dataAdapterDto.config().getClass().getPackage().getName();
        return pluginMetaData.stream()
                .filter(metaData -> packageName.startsWith(metaData.getClass().getPackage().getName()))
                .map(PluginVersionConstraint::of)
                .collect(Collectors.toSet());
    }

    @Override
    public NativeEntity<DataAdapterDto> createNativeEntity(Entity entity,
                                                           Map<String, ValueReference> parameters,
                                                           Map<EntityDescriptor, Object> nativeEntities,
                                                           String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<DataAdapterDto> decode(EntityV1 entity, final Map<String, ValueReference> parameters) {
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entity.data(), LookupDataAdapterEntity.class);
        final LookupDataAdapterConfiguration configuration = objectMapper.convertValue(toValueMap(lookupDataAdapterEntity.configuration(), parameters), LookupDataAdapterConfiguration.class);
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .name(lookupDataAdapterEntity.name().asString(parameters))
                .title(lookupDataAdapterEntity.title().asString(parameters))
                .description(lookupDataAdapterEntity.description().asString(parameters))
                .config(configuration)
                .build();

        final DataAdapterDto savedDataAdapterDto = dataAdapterService.save(dataAdapterDto);
        return NativeEntity.create(entity.id(), savedDataAdapterDto.id(), TYPE_V1, savedDataAdapterDto.title(), savedDataAdapterDto);
    }

    @Override
    public Optional<NativeEntity<DataAdapterDto>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        if (entity instanceof EntityV1) {
            return findExisting((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<DataAdapterDto>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final LookupDataAdapterEntity dataAdapterEntity = objectMapper.convertValue(entity.data(), LookupDataAdapterEntity.class);
        final String name = dataAdapterEntity.name().asString(parameters);

        final Optional<DataAdapterDto> existingDataAdapter = dataAdapterService.get(name);

        return existingDataAdapter.map(dataAdapter -> NativeEntity.create(entity.id(), dataAdapter.id(), TYPE_V1, dataAdapter.title(), dataAdapter));
    }

    @Override
    public Optional<NativeEntity<DataAdapterDto>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return dataAdapterService.get(nativeEntityDescriptor.id().id())
                .map(entity -> NativeEntity.create(nativeEntityDescriptor, entity));
    }

    @Override
    public void delete(DataAdapterDto nativeEntity) {
        dataAdapterService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(DataAdapterDto dataAdapterDto) {
        return EntityExcerpt.builder()
                .id(ModelId.of(dataAdapterDto.id()))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .title(dataAdapterDto.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return dataAdapterService.findAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        return dataAdapterService.get(modelId.id()).map(dataAdapterDto -> exportNativeEntity(dataAdapterDto, entityDescriptorIds));
    }
}
