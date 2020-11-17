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
import org.graylog2.contentpacks.model.entities.LookupCacheEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.lookup.db.DBCacheService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toReferenceMap;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toValueMap;

public class LookupCacheFacade implements EntityFacade<CacheDto> {
    public static final ModelType TYPE_V1 = ModelTypes.LOOKUP_CACHE_V1;

    private final ObjectMapper objectMapper;
    private final DBCacheService cacheService;
    private final Set<PluginMetaData> pluginMetaData;

    @Inject
    public LookupCacheFacade(ObjectMapper objectMapper,
                             DBCacheService cacheService,
                             Set<PluginMetaData> pluginMetaData) {
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.pluginMetaData = pluginMetaData;
    }

    @VisibleForTesting
    Entity exportNativeEntity(CacheDto cacheDto, EntityDescriptorIds entityDescriptorIds) {
        // TODO: Create independent representation of entity?
        final Map<String, Object> configuration = objectMapper.convertValue(cacheDto.config(), TypeReferences.MAP_STRING_OBJECT);
        final LookupCacheEntity lookupCacheEntity = LookupCacheEntity.create(
                ValueReference.of(cacheDto.name()),
                ValueReference.of(cacheDto.title()),
                ValueReference.of(cacheDto.description()),
                toReferenceMap(configuration));
        final JsonNode data = objectMapper.convertValue(lookupCacheEntity, JsonNode.class);
        final Set<Constraint> constraints = versionConstraints(cacheDto);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(cacheDto.id(), ModelTypes.LOOKUP_CACHE_V1)))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .constraints(ImmutableSet.copyOf(constraints))
                .data(data)
                .build();
    }

    private Set<Constraint> versionConstraints(CacheDto cacheDto) {
        // TODO: Find more robust method of identifying the providing plugin
        final String packageName = cacheDto.config().getClass().getPackage().getName();

        return pluginMetaData.stream()
                .filter(metaData -> packageName.startsWith(metaData.getClass().getPackage().getName()))
                .map(PluginVersionConstraint::of)
                .collect(Collectors.toSet());
    }

    @Override
    public NativeEntity<CacheDto> createNativeEntity(Entity entity,
                                                     Map<String, ValueReference> parameters,
                                                     Map<EntityDescriptor, Object> nativeEntities,
                                                     String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<CacheDto> decode(EntityV1 entity, Map<String, ValueReference> parameters) {
        final LookupCacheEntity lookupCacheEntity = objectMapper.convertValue(entity.data(), LookupCacheEntity.class);
        final LookupCacheConfiguration configuration = objectMapper.convertValue(toValueMap(lookupCacheEntity.configuration(), parameters), LookupCacheConfiguration.class);
        final CacheDto cacheDto = CacheDto.builder()
                .name(lookupCacheEntity.name().asString(parameters))
                .title(lookupCacheEntity.title().asString(parameters))
                .description(lookupCacheEntity.description().asString(parameters))
                .config(configuration)
                .build();

        final CacheDto savedCacheDto = cacheService.save(cacheDto);
        return NativeEntity.create(entity.id(), savedCacheDto.id(), TYPE_V1, savedCacheDto.title(), savedCacheDto);
    }

    @Override
    public Optional<NativeEntity<CacheDto>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        if (entity instanceof EntityV1) {
            return findExisting((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<CacheDto>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final LookupCacheEntity cacheEntity = objectMapper.convertValue(entity.data(), LookupCacheEntity.class);
        final String name = cacheEntity.name().asString(parameters);

        final Optional<CacheDto> existingCache = cacheService.get(name);

        return existingCache.map(cache -> NativeEntity.create(entity.id(), cache.id(), TYPE_V1, cache.title(), cache));
    }

    @Override
    public Optional<NativeEntity<CacheDto>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return cacheService.get(nativeEntityDescriptor.id().id())
                .map(entity -> NativeEntity.create(nativeEntityDescriptor, entity));
    }

    @Override
    public void delete(CacheDto nativeEntity) {
        cacheService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(CacheDto cacheDto) {
        return EntityExcerpt.builder()
                .id(ModelId.of(cacheDto.id()))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .title(cacheDto.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return cacheService.findAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        return cacheService.get(modelId.id()).map(cacheDto -> exportNativeEntity(cacheDto, entityDescriptorIds));
    }
}
