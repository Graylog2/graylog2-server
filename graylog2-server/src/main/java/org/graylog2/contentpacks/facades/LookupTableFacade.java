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
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.exceptions.DivergingEntityConfigurationException;
import org.graylog2.contentpacks.exceptions.MissingNativeEntityException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.LookupTableEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.lookup.LookupDefaultMultiValue;
import org.graylog2.lookup.LookupDefaultSingleValue;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupTableFacade implements EntityFacade<LookupTableDto> {
    public static final ModelType TYPE_V1 = ModelTypes.LOOKUP_TABLE_V1;

    private final ObjectMapper objectMapper;
    private final DBLookupTableService lookupTableService;

    @Inject
    public LookupTableFacade(ObjectMapper objectMapper,
                             DBLookupTableService lookupTableService) {
        this.objectMapper = objectMapper;
        this.lookupTableService = lookupTableService;
    }

    private EntityDescriptor adapterDescriptor(String adapterId) {
        return EntityDescriptor.create(adapterId, ModelTypes.LOOKUP_ADAPTER_V1);
    }

    private EntityDescriptor cacheDescriptor(String cacheId) {
        return EntityDescriptor.create(cacheId, ModelTypes.LOOKUP_CACHE_V1);
    }

    @VisibleForTesting
    Entity exportNativeEntity(LookupTableDto lookupTableDto, EntityDescriptorIds entityDescriptorIds) {
        final String tableId = entityDescriptorIds.get(EntityDescriptor.create(lookupTableDto.id(), ModelTypes.LOOKUP_TABLE_V1))
                .orElseThrow(() -> new ContentPackException("Couldn't find lookup table entity " + lookupTableDto.id()));
        final String cacheId = entityDescriptorIds.get(cacheDescriptor(lookupTableDto.cacheId()))
                .orElseThrow(() -> new ContentPackException("Couldn't find lookup cache entity " + lookupTableDto.cacheId()));
        final String adapterId = entityDescriptorIds.get(adapterDescriptor(lookupTableDto.dataAdapterId()))
                .orElseThrow(() -> new ContentPackException("Couldn't find lookup data adapter entity " + lookupTableDto.dataAdapterId()));

        final LookupTableEntity lookupTableEntity = LookupTableEntity.create(
                ValueReference.of(lookupTableDto.name()),
                ValueReference.of(lookupTableDto.title()),
                ValueReference.of(lookupTableDto.description()),
                ValueReference.of(cacheId),
                ValueReference.of(adapterId),
                ValueReference.of(lookupTableDto.defaultSingleValue()),
                ValueReference.of(lookupTableDto.defaultSingleValueType()),
                ValueReference.of(lookupTableDto.defaultMultiValue()),
                ValueReference.of(lookupTableDto.defaultMultiValueType()));
        final JsonNode data = objectMapper.convertValue(lookupTableEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(tableId))
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .data(data)
                .build();
    }

    @Override
    public NativeEntity<LookupTableDto> createNativeEntity(Entity entity,
                                                           Map<String, ValueReference> parameters,
                                                           Map<EntityDescriptor, Object> nativeEntities,
                                                           String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<LookupTableDto> decode(EntityV1 entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Object> nativeEntities) {
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);


        final String referencedDataAdapterName = lookupTableEntity.dataAdapterName().asString(parameters);
        final EntityDescriptor dataAdapterDescriptor = adapterDescriptor(referencedDataAdapterName);
        final Object dataAdapter = nativeEntities.get(dataAdapterDescriptor);
        final String dataAdapterId;
        if (dataAdapter instanceof DataAdapterDto) {
            dataAdapterId = ((DataAdapterDto) dataAdapter).id();
        } else {
            throw new MissingNativeEntityException(dataAdapterDescriptor);
        }

        final String referencedCacheName = lookupTableEntity.cacheName().asString(parameters);
        final EntityDescriptor cacheDescriptor = cacheDescriptor(referencedCacheName);
        final Object cache = nativeEntities.get(cacheDescriptor);
        final String cacheId;
        if (cache instanceof CacheDto) {
            cacheId = ((CacheDto) cache).id();
        } else {
            throw new MissingNativeEntityException(cacheDescriptor);
        }
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .name(lookupTableEntity.name().asString(parameters))
                .title(lookupTableEntity.title().asString(parameters))
                .description(lookupTableEntity.description().asString(parameters))
                .dataAdapterId(dataAdapterId)
                .cacheId(cacheId)
                .defaultSingleValue(lookupTableEntity.defaultSingleValue().asString(parameters))
                .defaultSingleValueType(lookupTableEntity.defaultSingleValueType().asEnum(parameters, LookupDefaultSingleValue.Type.class))
                .defaultMultiValue(lookupTableEntity.defaultMultiValue().asString(parameters))
                .defaultMultiValueType(lookupTableEntity.defaultMultiValueType().asEnum(parameters, LookupDefaultMultiValue.Type.class))
                .build();
        final LookupTableDto savedLookupTableDto = lookupTableService.save(lookupTableDto);
        return NativeEntity.create(entity.id(), savedLookupTableDto.id(), TYPE_V1, lookupTableDto.title(), savedLookupTableDto);
    }

    @Override
    public Optional<NativeEntity<LookupTableDto>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        if (entity instanceof EntityV1) {
            return findExisting((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<LookupTableDto>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);
        final String name = lookupTableEntity.name().asString(parameters);
        final String title = lookupTableEntity.title().asString(parameters);

        final Optional<LookupTableDto> lookupTable = lookupTableService.get(name);
        lookupTable.ifPresent(existingLookupTable -> compareLookupTable(name, title, existingLookupTable));

        return lookupTable.map(lt -> NativeEntity.create(entity.id(), lt.id(), TYPE_V1, lt.title(), lt));
    }

    private void compareLookupTable(String name, String title, LookupTableDto existingLookupTable) {
        if (!name.equals(existingLookupTable.name()) || !title.equals(existingLookupTable.title())) {
            throw new DivergingEntityConfigurationException("Different lookup table configuration with name \"" + name + "\"");
        }
    }

    @Override
    public Optional<NativeEntity<LookupTableDto>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return lookupTableService.get(nativeEntityDescriptor.id().id())
                .map(entity -> NativeEntity.create(nativeEntityDescriptor, entity));
    }

    @Override
    public void delete(LookupTableDto nativeEntity) {
        lookupTableService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(LookupTableDto lookupTableDto) {
        return EntityExcerpt.builder()
                .id(ModelId.of(lookupTableDto.id()))
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .title(lookupTableDto.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return lookupTableService.findAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        return lookupTableService.get(modelId.id()).map(lookupTableDto -> exportNativeEntity(lookupTableDto, entityDescriptorIds));
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Optional<LookupTableDto> lookupTableDto = lookupTableService.get(modelId.id());

        lookupTableDto.map(LookupTableDto::dataAdapterId)
                .map(this::adapterDescriptor)
                .ifPresent(dataAdapter -> mutableGraph.putEdge(entityDescriptor, dataAdapter));
        lookupTableDto.map(LookupTableDto::cacheId)
                .map(this::cacheDescriptor)
                .ifPresent(cache -> mutableGraph.putEdge(entityDescriptor, cache));

        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveForInstallation((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveForInstallation(EntityV1 entity,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);

        final String dataAdapterName = lookupTableEntity.dataAdapterName().asString(parameters);
        final EntityDescriptor dataAdapterDescriptor = adapterDescriptor(dataAdapterName);
        final Entity dataAdapterEntity = entities.get(dataAdapterDescriptor);
        if (dataAdapterEntity == null) {
            throw new ContentPackException("Missing data adapter \"" + dataAdapterName + "\" for lookup table " + entity.toEntityDescriptor());
        } else {
            mutableGraph.putEdge(entity, dataAdapterEntity);
        }

        final String cacheName = lookupTableEntity.cacheName().asString(parameters);
        final EntityDescriptor cacheDescriptor = cacheDescriptor(cacheName);
        final Entity cacheEntity = entities.get(cacheDescriptor);
        if (cacheEntity == null) {
            throw new ContentPackException("Missing cache \"" + cacheName + "\" for lookup table " + entity.toEntityDescriptor());
        } else {
            mutableGraph.putEdge(entity, cacheEntity);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
