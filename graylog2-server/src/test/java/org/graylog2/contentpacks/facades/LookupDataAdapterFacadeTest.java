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
import org.graylog2.contentpacks.model.entities.LookupDataAdapterEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.lookup.FallbackAdapterConfig;
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

public class LookupDataAdapterFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private LookupDataAdapterFacade facade;
    private DBDataAdapterService dataAdapterService;
    private Set<PluginMetaData> pluginMetaData;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        dataAdapterService = new DBDataAdapterService(
                mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);
        pluginMetaData = new HashSet<>();

        facade = new LookupDataAdapterFacade(objectMapper, dataAdapterService, pluginMetaData);
    }

    @Test
    public void exportNativeEntity() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final EntityDescriptor descriptor = EntityDescriptor.create(dataAdapterDto.id(), ModelTypes.LOOKUP_ADAPTER_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportNativeEntity(dataAdapterDto, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entityV1.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("data-adapter-name"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("Data Adapter Title"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("Data Adapter Description"));
        assertThat(lookupDataAdapterEntity.configuration()).containsEntry("type", ValueReference.of("FallbackAdapterConfig"));
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void exportEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportEntity(descriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entityV1.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("http-dsv"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("HTTP DSV"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("HTTP DSV"));
    }

    @Test
    public void createNativeEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("http-dsv"),
                        ValueReference.of("HTTP DSV"),
                        ValueReference.of("HTTP DSV"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        assertThat(dataAdapterService.findAll()).isEmpty();

        final NativeEntity<DataAdapterDto> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), Collections.emptyMap(), "username");

        assertThat(nativeEntity.descriptor().id()).isNotNull();
        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        assertThat(nativeEntity.entity().name()).isEqualTo("http-dsv");
        assertThat(nativeEntity.entity().title()).isEqualTo("HTTP DSV");
        assertThat(nativeEntity.entity().description()).isEqualTo("HTTP DSV");

        assertThat(dataAdapterService.findAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("http-dsv"),
                        ValueReference.of("HTTP DSV"),
                        ValueReference.of("HTTP DSV"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        final NativeEntity<DataAdapterDto> nativeEntity = facade.findExisting(entity, Collections.emptyMap()).orElseThrow(AssertionError::new);

        assertThat(nativeEntity.descriptor().id()).isEqualTo(ModelId.of("5adf24a04b900a0fdb4e52c8"));
        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        assertThat(nativeEntity.entity().name()).isEqualTo("http-dsv");
        assertThat(nativeEntity.entity().title()).isEqualTo("HTTP DSV");
        assertThat(nativeEntity.entity().description()).isEqualTo("HTTP DSV");
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void findExistingWithNoExistingEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("some-name"),
                        ValueReference.of("Some title"),
                        ValueReference.of("Some description"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        final Optional<NativeEntity<DataAdapterDto>> existingEntity = facade.findExisting(entity, Collections.emptyMap());
        assertThat(existingEntity).isEmpty();
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void delete() {
        final Optional<DataAdapterDto> dataAdapterDto = dataAdapterService.get("5adf24a04b900a0fdb4e52c8");

        assertThat(dataAdapterService.findAll()).hasSize(1);
        dataAdapterDto.ifPresent(facade::delete);

        assertThat(dataAdapterService.findAll()).isEmpty();
        assertThat(dataAdapterService.get("5adf24a04b900a0fdb4e52c8")).isEmpty();
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5adf24a04b900a0fdb4e52c8"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("http-dsv"),
                        ValueReference.of("HTTP DSV"),
                        ValueReference.of("HTTP DSV"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }


    @Test
    public void createExcerpt() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(dataAdapterDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        assertThat(excerpt.title()).isEqualTo("Data Adapter Title");
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5adf24a04b900a0fdb4e52c8"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .title("HTTP DSV")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @MongoDBFixtures("LookupDataAdapterFacadeTest.json")
    public void collectEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> collectedEntity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entity.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("http-dsv"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("HTTP DSV"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("HTTP DSV"));
    }
}
