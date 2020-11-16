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
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.services.CollectorService;
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
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.SidecarCollectorEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SidecarCollectorFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private CollectorService collectorService;
    private SidecarCollectorFacade facade;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        collectorService = new CollectorService(mongodb.mongoConnection(), mapperProvider);
        facade = new SidecarCollectorFacade(objectMapper, collectorService);
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void exportNativeEntity() {
        final Collector collector = collectorService.find("5b4c920b4b900a0024af0001");
        final EntityDescriptor descriptor = EntityDescriptor.create(collector.id(), ModelTypes.SIDECAR_COLLECTOR_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportNativeEntity(collector, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.SIDECAR_COLLECTOR_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final SidecarCollectorEntity collectorEntity = objectMapper.convertValue(entityV1.data(), SidecarCollectorEntity.class);

        assertThat(collectorEntity.name()).isEqualTo(ValueReference.of("filebeat"));
        assertThat(collectorEntity.serviceType()).isEqualTo(ValueReference.of("exec"));
        assertThat(collectorEntity.nodeOperatingSystem()).isEqualTo(ValueReference.of("linux"));
        assertThat(collectorEntity.executablePath()).isEqualTo(ValueReference.of("/usr/lib/graylog-sidecar/filebeat"));
        assertThat(collectorEntity.executeParameters()).isEqualTo(ValueReference.of("-c %s"));
        assertThat(collectorEntity.validationParameters()).isEqualTo(ValueReference.of("test config -c %s"));
        assertThat(collectorEntity.defaultTemplate()).isEqualTo(ValueReference.of(""));
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void exportEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5b4c920b4b900a0024af0001", ModelTypes.SIDECAR_COLLECTOR_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);

        final Entity entity = facade.exportEntity(descriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.SIDECAR_COLLECTOR_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final SidecarCollectorEntity collectorEntity = objectMapper.convertValue(entityV1.data(), SidecarCollectorEntity.class);

        assertThat(collectorEntity.name()).isEqualTo(ValueReference.of("filebeat"));
        assertThat(collectorEntity.serviceType()).isEqualTo(ValueReference.of("exec"));
        assertThat(collectorEntity.nodeOperatingSystem()).isEqualTo(ValueReference.of("linux"));
        assertThat(collectorEntity.executablePath()).isEqualTo(ValueReference.of("/usr/lib/graylog-sidecar/filebeat"));
        assertThat(collectorEntity.executeParameters()).isEqualTo(ValueReference.of("-c %s"));
        assertThat(collectorEntity.validationParameters()).isEqualTo(ValueReference.of("test config -c %s"));
        assertThat(collectorEntity.defaultTemplate()).isEqualTo(ValueReference.of(""));
    }

    @Test
    public void createNativeEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("0"))
                .type(ModelTypes.SIDECAR_COLLECTOR_V1)
                .data(objectMapper.convertValue(SidecarCollectorEntity.create(
                        ValueReference.of("filebeat"),
                        ValueReference.of("exec"),
                        ValueReference.of("linux"),
                        ValueReference.of("/usr/lib/graylog-sidecar/filebeat"),
                        ValueReference.of("-c %s"),
                        ValueReference.of("test config -c %s"),
                        ValueReference.of("")), JsonNode.class))
                .build();

        assertThat(collectorService.count()).isEqualTo(0L);

        final NativeEntity<Collector> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), Collections.emptyMap(), "username");
        assertThat(collectorService.count()).isEqualTo(1L);

        final Collector collector = collectorService.findByName("filebeat");
        assertThat(collector).isNotNull();

        final NativeEntityDescriptor expectedDescriptor = NativeEntityDescriptor.create(entity.id(), collector.id(), ModelTypes.SIDECAR_COLLECTOR_V1, collector.name(), false);
        assertThat(nativeEntity.descriptor()).isEqualTo(expectedDescriptor);
        assertThat(nativeEntity.entity()).isEqualTo(collector);
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("0"))
                .type(ModelTypes.SIDECAR_COLLECTOR_V1)
                .data(objectMapper.convertValue(SidecarCollectorEntity.create(
                        ValueReference.of("filebeat"),
                        ValueReference.of("exec"),
                        ValueReference.of("linux"),
                        ValueReference.of("/usr/lib/graylog-sidecar/filebeat"),
                        ValueReference.of("-c %s"),
                        ValueReference.of("test config -c %s"),
                        ValueReference.of("")), JsonNode.class))
                .build();

        final NativeEntity<Collector> existingCollector = facade.findExisting(entity, Collections.emptyMap())
                .orElseThrow(AssertionError::new);

        final Collector collector = collectorService.findByName("filebeat");
        assertThat(collector).isNotNull();

        final NativeEntityDescriptor expectedDescriptor = NativeEntityDescriptor.create(entity.id(), collector.id(), ModelTypes.SIDECAR_COLLECTOR_V1, collector.name(), false);
        assertThat(existingCollector.descriptor()).isEqualTo(expectedDescriptor);
        assertThat(existingCollector.entity()).isEqualTo(collector);
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void delete() {
        final Collector collector = collectorService.find("5b4c920b4b900a0024af0001");

        assertThat(collectorService.count()).isEqualTo(3L);
        facade.delete(collector);
        assertThat(collectorService.count()).isEqualTo(2L);
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void createExcerpt() {
        final Collector collector = collectorService.find("5b4c920b4b900a0024af0001");
        final EntityExcerpt excerpt = facade.createExcerpt(collector);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("5b4c920b4b900a0024af0001"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.SIDECAR_COLLECTOR_V1);
        assertThat(excerpt.title()).isEqualTo("filebeat");
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void listEntityExcerpts() {
        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(
                EntityExcerpt.builder()
                        .id(ModelId.of("5b4c920b4b900a0024af0001"))
                        .type(ModelTypes.SIDECAR_COLLECTOR_V1)
                        .title("filebeat")
                        .build(),
                EntityExcerpt.builder()
                        .id(ModelId.of("5b4c920b4b900a0024af0002"))
                        .type(ModelTypes.SIDECAR_COLLECTOR_V1)
                        .title("winlogbeat")
                        .build(),
                EntityExcerpt.builder()
                        .id(ModelId.of("5b4c920b4b900a0024af0003"))
                        .type(ModelTypes.SIDECAR_COLLECTOR_V1)
                        .title("nxlog")
                        .build()
        );
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5b4c920b4b900a0024af0001", ModelTypes.SIDECAR_COLLECTOR_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    @MongoDBFixtures("SidecarCollectorFacadeTest.json")
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("0"))
                .type(ModelTypes.SIDECAR_COLLECTOR_V1)
                .data(objectMapper.convertValue(SidecarCollectorEntity.create(
                        ValueReference.of("filebeat"),
                        ValueReference.of("exec"),
                        ValueReference.of("linux"),
                        ValueReference.of("/usr/lib/graylog-sidecar/filebeat"),
                        ValueReference.of("-c %s"),
                        ValueReference.of("test config -c %s"),
                        ValueReference.of("")), JsonNode.class))
                .build();

        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }
}
