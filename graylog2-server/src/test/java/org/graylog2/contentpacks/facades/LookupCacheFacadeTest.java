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
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Graph;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.LookupCacheEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.db.DBCacheService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.lookup.FallbackCacheConfig;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupCacheFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private LookupCacheFacade facade;
    private DBCacheService cacheService;
    private Set<PluginMetaData> pluginMetaData;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        cacheService = new DBCacheService(
                mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);
        pluginMetaData = new HashSet<>();

        facade = new LookupCacheFacade(objectMapper, cacheService, pluginMetaData);
    }

    @Test
    public void exportNativeEntity() {
        final CacheDto cacheDto = CacheDto.builder()
                .id("1234567890")
                .name("cache-name")
                .title("Cache Title")
                .description("Cache Description")
                .config(new FallbackCacheConfig())
                .build();
        final EntityDescriptor descriptor = EntityDescriptor.create(cacheDto.id(), ModelTypes.LOOKUP_CACHE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportNativeEntity(cacheDto, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_CACHE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupCacheEntity lookupCacheEntity = objectMapper.convertValue(entityV1.data(), LookupCacheEntity.class);
        assertThat(lookupCacheEntity.name()).isEqualTo(ValueReference.of("cache-name"));
        assertThat(lookupCacheEntity.title()).isEqualTo(ValueReference.of("Cache Title"));
        assertThat(lookupCacheEntity.description()).isEqualTo(ValueReference.of("Cache Description"));
        assertThat(lookupCacheEntity.configuration()).containsEntry("type", ValueReference.of("FallbackCacheConfig"));
    }

    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void exportEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24b24b900a0fdb4e52dd", ModelTypes.LOOKUP_CACHE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportEntity(descriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_CACHE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupCacheEntity lookupCacheEntity = objectMapper.convertValue(entityV1.data(), LookupCacheEntity.class);
        assertThat(lookupCacheEntity.name()).isEqualTo(ValueReference.of("no-op-cache"));
        assertThat(lookupCacheEntity.title()).isEqualTo(ValueReference.of("No-op cache"));
        assertThat(lookupCacheEntity.description()).isEqualTo(ValueReference.of("No-op cache"));
        assertThat(lookupCacheEntity.configuration()).containsEntry("type", ValueReference.of("none"));
    }

    @Test
    public void createNativeEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .data(objectMapper.convertValue(LookupCacheEntity.create(
                        ValueReference.of("no-op-cache"),
                        ValueReference.of("No-op cache"),
                        ValueReference.of("No-op cache"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("type", "none"))
                ), JsonNode.class))
                .build();
        assertThat(cacheService.findAll()).isEmpty();

        final NativeEntity<CacheDto> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), Collections.emptyMap(), "username");
        final NativeEntityDescriptor descriptor = nativeEntity.descriptor();
        final CacheDto cacheDto = nativeEntity.entity();

        assertThat(nativeEntity.descriptor().id()).isNotNull();
        assertThat(descriptor.type()).isEqualTo(ModelTypes.LOOKUP_CACHE_V1);
        assertThat(cacheDto.name()).isEqualTo("no-op-cache");
        assertThat(cacheDto.title()).isEqualTo("No-op cache");
        assertThat(cacheDto.description()).isEqualTo("No-op cache");
        assertThat(cacheDto.config().type()).isEqualTo("none");

        assertThat(cacheService.findAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .data(objectMapper.convertValue(LookupCacheEntity.create(
                        ValueReference.of("no-op-cache"),
                        ValueReference.of("No-op cache"),
                        ValueReference.of("No-op cache"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("type", "none"))
                ), JsonNode.class))
                .build();

        final NativeEntity<CacheDto> existingCache = facade.findExisting(entity, Collections.emptyMap())
                .orElseThrow(AssertionError::new);
        final NativeEntityDescriptor descriptor = existingCache.descriptor();
        final CacheDto cacheDto = existingCache.entity();

        assertThat(descriptor.id()).isEqualTo(ModelId.of("5adf24b24b900a0fdb4e52dd"));
        assertThat(descriptor.type()).isEqualTo(ModelTypes.LOOKUP_CACHE_V1);
        assertThat(cacheDto.id()).isEqualTo("5adf24b24b900a0fdb4e52dd");
        assertThat(cacheDto.name()).isEqualTo("no-op-cache");
        assertThat(cacheDto.title()).isEqualTo("No-op cache");
        assertThat(cacheDto.description()).isEqualTo("No-op cache");
        assertThat(cacheDto.config().type()).isEqualTo("none");
    }

    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void findExistingWithNoExistingEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .data(objectMapper.convertValue(LookupCacheEntity.create(
                        ValueReference.of("some-cache"),
                        ValueReference.of("Some cache"),
                        ValueReference.of("Some cache"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("type", "none"))
                ), JsonNode.class))
                .build();

        final Optional<NativeEntity<CacheDto>> existingCache = facade.findExisting(entity, Collections.emptyMap());

        assertThat(existingCache).isEmpty();
    }

    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .data(objectMapper.convertValue(LookupCacheEntity.create(
                        ValueReference.of("no-op-cache"),
                        ValueReference.of("No-op cache"),
                        ValueReference.of("No-op cache"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("type", "none"))
                ), JsonNode.class))
                .build();

        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }


    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24b24b900a0fdb4e52dd", ModelTypes.LOOKUP_CACHE_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void delete() {
        final Optional<CacheDto> cacheDto = cacheService.get("5adf24b24b900a0fdb4e52dd");

        assertThat(cacheService.findAll()).hasSize(1);
        cacheDto.ifPresent(facade::delete);

        assertThat(cacheService.get("5adf24b24b900a0fdb4e52dd")).isEmpty();
        assertThat(cacheService.findAll()).isEmpty();
    }

    @Test
    public void createExcerpt() {
        final CacheDto cacheDto = CacheDto.builder()
                .id("1234567890")
                .name("cache-name")
                .title("Cache Title")
                .description("Cache Description")
                .config(new FallbackCacheConfig())
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(cacheDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.LOOKUP_CACHE_V1);
        assertThat(excerpt.title()).isEqualTo("Cache Title");
    }

    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5adf24b24b900a0fdb4e52dd"))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .title("No-op cache")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @MongoDBFixtures("LookupCacheFacadeTest.json")
    public void collectEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24b24b900a0fdb4e52dd", ModelTypes.LOOKUP_CACHE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> collectedEntity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_CACHE_V1);
        final LookupCacheEntity lookupCacheEntity = objectMapper.convertValue(entity.data(), LookupCacheEntity.class);
        assertThat(lookupCacheEntity.name()).isEqualTo(ValueReference.of("no-op-cache"));
        assertThat(lookupCacheEntity.title()).isEqualTo(ValueReference.of("No-op cache"));
        assertThat(lookupCacheEntity.description()).isEqualTo(ValueReference.of("No-op cache"));
    }
}
