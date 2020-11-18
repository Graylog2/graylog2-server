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
import org.graylog2.contentpacks.model.entities.LookupDataAdapterEntity;
import org.graylog2.contentpacks.model.entities.LookupTableEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.LookupDefaultValue;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.plugin.lookup.FallbackAdapterConfig;
import org.graylog2.plugin.lookup.FallbackCacheConfig;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupTableFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private DBLookupTableService lookupTableService;
    private LookupTableFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        lookupTableService = new DBLookupTableService(
                mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);

        facade = new LookupTableFacade(objectMapper, lookupTableService);
    }

    @Test
    public void exportEntity() {
        final EntityDescriptor tableDescriptor = EntityDescriptor.create("1234567890", ModelTypes.LOOKUP_TABLE_V1);
        final EntityDescriptor adapterDescriptor = EntityDescriptor.create("data-adapter-1234", ModelTypes.LOOKUP_ADAPTER_V1);
        final EntityDescriptor cacheDescriptor = EntityDescriptor.create("cache-1234", ModelTypes.LOOKUP_CACHE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(
                tableDescriptor,
                adapterDescriptor,
                cacheDescriptor
        );
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .id("1234567890")
                .name("lookup-table-name")
                .title("Lookup Table Title")
                .description("Lookup Table Description")
                .dataAdapterId("data-adapter-1234")
                .cacheId("cache-1234")
                .defaultSingleValue("default-single")
                .defaultSingleValueType(LookupDefaultValue.Type.STRING)
                .defaultMultiValue("default-multi")
                .defaultMultiValueType(LookupDefaultValue.Type.STRING)
                .build();
        final Entity entity = facade.exportNativeEntity(lookupTableDto, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(tableDescriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_TABLE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entityV1.data(), LookupTableEntity.class);
        assertThat(lookupTableEntity.name()).isEqualTo(ValueReference.of("lookup-table-name"));
        assertThat(lookupTableEntity.title()).isEqualTo(ValueReference.of("Lookup Table Title"));
        assertThat(lookupTableEntity.description()).isEqualTo(ValueReference.of("Lookup Table Description"));
        assertThat(lookupTableEntity.dataAdapterName()).isEqualTo(ValueReference.of(entityDescriptorIds.get(adapterDescriptor).orElse(null)));
        assertThat(lookupTableEntity.cacheName()).isEqualTo(ValueReference.of(entityDescriptorIds.get(cacheDescriptor).orElse(null)));
        assertThat(lookupTableEntity.defaultSingleValue()).isEqualTo(ValueReference.of("default-single"));
        assertThat(lookupTableEntity.defaultSingleValueType()).isEqualTo(ValueReference.of(LookupDefaultValue.Type.STRING));
        assertThat(lookupTableEntity.defaultMultiValue()).isEqualTo(ValueReference.of("default-multi"));
        assertThat(lookupTableEntity.defaultMultiValueType()).isEqualTo(ValueReference.of(LookupDefaultValue.Type.STRING));
    }

    @Test
    @MongoDBFixtures("LookupTableFacadeTest.json")
    public void exportNativeEntity() {
        final EntityDescriptor tableDescriptor = EntityDescriptor.create("5adf24dd4b900a0fdb4e530d", ModelTypes.LOOKUP_TABLE_V1);
        final EntityDescriptor adapterDescriptor = EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1);
        final EntityDescriptor cacheDescriptor = EntityDescriptor.create("5adf24b24b900a0fdb4e52dd", ModelTypes.LOOKUP_CACHE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(
                tableDescriptor,
                adapterDescriptor,
                cacheDescriptor
        );
        final Entity entity = facade.exportEntity(tableDescriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(tableDescriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_TABLE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entityV1.data(), LookupTableEntity.class);
        assertThat(lookupTableEntity.name()).isEqualTo(ValueReference.of("http-dsv-no-cache"));
        assertThat(lookupTableEntity.title()).isEqualTo(ValueReference.of("HTTP DSV without Cache"));
        assertThat(lookupTableEntity.description()).isEqualTo(ValueReference.of("HTTP DSV without Cache"));
        assertThat(lookupTableEntity.dataAdapterName()).isEqualTo(ValueReference.of(entityDescriptorIds.get(adapterDescriptor).orElse(null)));
        assertThat(lookupTableEntity.cacheName()).isEqualTo(ValueReference.of(entityDescriptorIds.get(cacheDescriptor).orElse(null)));
        assertThat(lookupTableEntity.defaultSingleValue()).isEqualTo(ValueReference.of("Default single value"));
        assertThat(lookupTableEntity.defaultSingleValueType()).isEqualTo(ValueReference.of(LookupDefaultValue.Type.STRING));
        assertThat(lookupTableEntity.defaultMultiValue()).isEqualTo(ValueReference.of("Default multi value"));
        assertThat(lookupTableEntity.defaultMultiValueType()).isEqualTo(ValueReference.of(LookupDefaultValue.Type.OBJECT));
    }

    @Test
    public void createNativeEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .data(objectMapper.convertValue(LookupTableEntity.create(
                        ValueReference.of("http-dsv-no-cache"),
                        ValueReference.of("HTTP DSV without Cache"),
                        ValueReference.of("HTTP DSV without Cache"),
                        ValueReference.of("no-op-cache"),
                        ValueReference.of("http-dsv"),
                        ValueReference.of("Default single value"),
                        ValueReference.of(LookupDefaultValue.Type.STRING),
                        ValueReference.of("Default multi value"),
                        ValueReference.of(LookupDefaultValue.Type.OBJECT)), JsonNode.class))
                .build();
        final EntityDescriptor cacheDescriptor = EntityDescriptor.create("no-op-cache", ModelTypes.LOOKUP_CACHE_V1);
        final CacheDto cacheDto = CacheDto.builder()
                .id("5adf24b24b900a0fdb4e0001")
                .name("no-op-cache")
                .title("No-op cache")
                .description("No-op cache")
                .config(new FallbackCacheConfig())
                .build();
        final EntityDescriptor dataAdapterDescriptor = EntityDescriptor.create("http-dsv", ModelTypes.LOOKUP_ADAPTER_V1);
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("5adf24a04b900a0fdb4e0002")
                .name("http-dsv")
                .title("HTTP DSV")
                .description("HTTP DSV")
                .config(new FallbackAdapterConfig())
                .build();
        final Map<EntityDescriptor, Object> nativeEntities = ImmutableMap.of(
                cacheDescriptor, cacheDto,
                dataAdapterDescriptor, dataAdapterDto);
        assertThat(lookupTableService.findAll()).isEmpty();

        final NativeEntity<LookupTableDto> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), nativeEntities, "username");

        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.LOOKUP_TABLE_V1);
        assertThat(nativeEntity.entity().name()).isEqualTo("http-dsv-no-cache");
        assertThat(nativeEntity.entity().title()).isEqualTo("HTTP DSV without Cache");
        assertThat(nativeEntity.entity().description()).isEqualTo("HTTP DSV without Cache");
        assertThat(nativeEntity.entity().cacheId()).isEqualTo("5adf24b24b900a0fdb4e0001");
        assertThat(nativeEntity.entity().dataAdapterId()).isEqualTo("5adf24a04b900a0fdb4e0002");
        assertThat(nativeEntity.entity().defaultSingleValue()).isEqualTo("Default single value");
        assertThat(nativeEntity.entity().defaultSingleValueType()).isEqualTo(LookupDefaultValue.Type.STRING);
        assertThat(nativeEntity.entity().defaultMultiValue()).isEqualTo("Default multi value");
        assertThat(nativeEntity.entity().defaultMultiValueType()).isEqualTo(LookupDefaultValue.Type.OBJECT);

        assertThat(lookupTableService.findAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("LookupTableFacadeTest.json")
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .data(objectMapper.convertValue(LookupTableEntity.create(
                        ValueReference.of("http-dsv-no-cache"),
                        ValueReference.of("HTTP DSV without Cache"),
                        ValueReference.of("HTTP DSV without Cache"),
                        ValueReference.of("cache-id"),
                        ValueReference.of("data-adapter-id"),
                        ValueReference.of("Default single value"),
                        ValueReference.of(LookupDefaultValue.Type.STRING),
                        ValueReference.of("Default multi value"),
                        ValueReference.of(LookupDefaultValue.Type.OBJECT)), JsonNode.class))
                .build();
        final NativeEntity<LookupTableDto> existingEntity = facade.findExisting(entity, Collections.emptyMap()).orElseThrow(AssertionError::new);

        assertThat(existingEntity.descriptor().id()).isEqualTo(ModelId.of("5adf24dd4b900a0fdb4e530d"));
        assertThat(existingEntity.descriptor().type()).isEqualTo(ModelTypes.LOOKUP_TABLE_V1);
        assertThat(existingEntity.entity().name()).isEqualTo("http-dsv-no-cache");
        assertThat(existingEntity.entity().title()).isEqualTo("HTTP DSV without Cache");
        assertThat(existingEntity.entity().description()).isEqualTo("HTTP DSV without Cache");
        assertThat(existingEntity.entity().dataAdapterId()).isEqualTo("5adf24a04b900a0fdb4e52c8");
        assertThat(existingEntity.entity().cacheId()).isEqualTo("5adf24b24b900a0fdb4e52dd");
        assertThat(existingEntity.entity().defaultSingleValue()).isEqualTo("Default single value");
        assertThat(existingEntity.entity().defaultSingleValueType()).isEqualTo(LookupDefaultValue.Type.STRING);
        assertThat(existingEntity.entity().defaultMultiValue()).isEqualTo("Default multi value");
        assertThat(existingEntity.entity().defaultMultiValueType()).isEqualTo(LookupDefaultValue.Type.OBJECT);
    }

    @Test
    @MongoDBFixtures({"LookupCacheFacadeTest.json", "LookupDataAdapterFacadeTest.json", "LookupTableFacadeTest.json"})
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5adf24dd4b900a0fdb4e530d"))
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .data(objectMapper.convertValue(LookupTableEntity.create(
                        ValueReference.of("http-dsv-no-cache"),
                        ValueReference.of("HTTP DSV without Cache"),
                        ValueReference.of("HTTP DSV without Cache"),
                        ValueReference.of("5adf24b24b900a0fdb4e52dd"),
                        ValueReference.of("5adf24a04b900a0fdb4e52c8"),
                        ValueReference.of("Default single value"),
                        ValueReference.of(LookupDefaultValue.Type.STRING),
                        ValueReference.of("Default multi value"),
                        ValueReference.of(LookupDefaultValue.Type.OBJECT)), JsonNode.class))
                .build();
        final Entity cacheEntity = EntityV1.builder()
                .id(ModelId.of("5adf24b24b900a0fdb4e52dd"))
                .type(ModelTypes.LOOKUP_CACHE_V1)
                .data(objectMapper.convertValue(LookupCacheEntity.create(
                        ValueReference.of("no-op-cache"),
                        ValueReference.of("No-op cache"),
                        ValueReference.of("No-op cache"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("type", "none"))
                ), JsonNode.class))
                .build();
        final Entity dataAdapterEntity = EntityV1.builder()
                .id(ModelId.of("5adf24a04b900a0fdb4e52c8"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("http-dsv"),
                        ValueReference.of("HTTP DSV"),
                        ValueReference.of("HTTP DSV"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        final Map<EntityDescriptor, Entity> entities = ImmutableMap.of(
                cacheEntity.toEntityDescriptor(), cacheEntity,
                dataAdapterEntity.toEntityDescriptor(), dataAdapterEntity);
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), entities);

        assertThat(graph.nodes())
                .hasSize(3)
                .containsOnly(entity, cacheEntity, dataAdapterEntity);
    }

    @Test
    @MongoDBFixtures({"LookupCacheFacadeTest.json", "LookupDataAdapterFacadeTest.json", "LookupTableFacadeTest.json"})
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24dd4b900a0fdb4e530d", ModelTypes.LOOKUP_TABLE_V1);

        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes())
                .hasSize(3)
                .containsOnly(
                        descriptor,
                        EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1),
                        EntityDescriptor.create("5adf24b24b900a0fdb4e52dd", ModelTypes.LOOKUP_CACHE_V1));
    }

    @Test
    @MongoDBFixtures("LookupTableFacadeTest.json")
    public void findExistingWithNoExistingEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .data(objectMapper.convertValue(LookupTableEntity.create(
                        ValueReference.of("some-name"),
                        ValueReference.of("Title"),
                        ValueReference.of("Description"),
                        ValueReference.of("cache-id"),
                        ValueReference.of("data-adapter-id"),
                        ValueReference.of("Default single value"),
                        ValueReference.of(LookupDefaultValue.Type.STRING),
                        ValueReference.of("Default multi value"),
                        ValueReference.of(LookupDefaultValue.Type.OBJECT)), JsonNode.class))
                .build();
        final Optional<NativeEntity<LookupTableDto>> existingEntity = facade.findExisting(entity, Collections.emptyMap());

        assertThat(existingEntity).isEmpty();
    }

    @Test
    @MongoDBFixtures("LookupTableFacadeTest.json")
    public void delete() {
        final Optional<LookupTableDto> lookupTableDto = lookupTableService.get("5adf24dd4b900a0fdb4e530d");

        assertThat(lookupTableService.findAll()).hasSize(1);
        lookupTableDto.ifPresent(facade::delete);

        assertThat(lookupTableService.findAll()).isEmpty();
        assertThat(lookupTableService.get("5adf24dd4b900a0fdb4e530d")).isEmpty();
    }

    @Test
    public void createExcerpt() {
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .id("1234567890")
                .name("lookup-table-name")
                .title("Lookup Table Title")
                .description("Lookup Table Description")
                .dataAdapterId("data-adapter-1234")
                .cacheId("cache-1234")
                .defaultSingleValue("default-single")
                .defaultSingleValueType(LookupDefaultValue.Type.STRING)
                .defaultMultiValue("default-multi")
                .defaultMultiValueType(LookupDefaultValue.Type.STRING)
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(lookupTableDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.LOOKUP_TABLE_V1);
        assertThat(excerpt.title()).isEqualTo("Lookup Table Title");
    }

    @Test
    @MongoDBFixtures("LookupTableFacadeTest.json")
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5adf24dd4b900a0fdb4e530d"))
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .title("HTTP DSV without Cache")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @MongoDBFixtures("LookupTableFacadeTest.json")
    public void collectEntity() {
        final EntityDescriptor tableDescriptor = EntityDescriptor.create("5adf24dd4b900a0fdb4e530d", ModelTypes.LOOKUP_TABLE_V1);
        final EntityDescriptor adapterDescriptor = EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1);
        final EntityDescriptor cacheDescriptor = EntityDescriptor.create("5adf24b24b900a0fdb4e52dd", ModelTypes.LOOKUP_CACHE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(
                tableDescriptor,
                adapterDescriptor,
                cacheDescriptor
        );
        final Optional<Entity> collectedEntity = facade.exportEntity(EntityDescriptor.create("http-dsv-no-cache", ModelTypes.LOOKUP_TABLE_V1), entityDescriptorIds);
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(tableDescriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_TABLE_V1);
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);
        assertThat(lookupTableEntity.name()).isEqualTo(ValueReference.of("http-dsv-no-cache"));
        assertThat(lookupTableEntity.title()).isEqualTo(ValueReference.of("HTTP DSV without Cache"));
        assertThat(lookupTableEntity.description()).isEqualTo(ValueReference.of("HTTP DSV without Cache"));
        assertThat(lookupTableEntity.dataAdapterName()).isEqualTo(ValueReference.of(entityDescriptorIds.get(adapterDescriptor).orElse(null)));
        assertThat(lookupTableEntity.cacheName()).isEqualTo(ValueReference.of(entityDescriptorIds.get(cacheDescriptor).orElse(null)));
    }
}
