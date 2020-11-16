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
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.OutputEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.outputs.LoggingOutput;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.OutputServiceImpl;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutputFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private StreamService streamService;
    @Mock
    private OutputRegistry outputRegistry;
    private Set<PluginMetaData> pluginMetaData;
    private OutputService outputService;
    private OutputFacade facade;
    private Map<String, MessageOutput.Factory<? extends MessageOutput>> outputFactories;
    private Map<String, MessageOutput.Factory2<? extends MessageOutput>> outputFactories2;

    @Before
    public void setUp() throws Exception {
        outputService = new OutputServiceImpl(mongodb.mongoConnection(), new MongoJackObjectMapperProvider(objectMapper), streamService, outputRegistry);
        pluginMetaData = new HashSet<>();
        outputFactories = new HashMap<>();
        outputFactories2 = new HashMap<>();
        final LoggingOutput.Factory factory = mock(LoggingOutput.Factory.class);
        final LoggingOutput.Descriptor descriptor = mock(LoggingOutput.Descriptor.class);
        when(factory.getDescriptor()).thenReturn(descriptor);
        outputFactories.put("org.graylog2.outputs.LoggingOutput", factory);

        facade = new OutputFacade(objectMapper, outputService, pluginMetaData, outputFactories, outputFactories2);
    }

    @Test
    public void exportEntity() {
        final ImmutableMap<String, Object> configuration = ImmutableMap.of(
                "some-setting", "foobar"
        );
        final OutputImpl output = OutputImpl.create(
                "01234567890",
                "Output Title",
                "org.graylog2.outputs.LoggingOutput",
                "admin",
                configuration,
                new Date(0L),
                null
        );
        final EntityDescriptor descriptor = EntityDescriptor.create(output.getId(), ModelTypes.OUTPUT_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportNativeEntity(output, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.OUTPUT_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final OutputEntity outputEntity = objectMapper.convertValue(entityV1.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo(ValueReference.of("Output Title"));
        assertThat(outputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.outputs.LoggingOutput"));
        assertThat(outputEntity.configuration()).containsEntry("some-setting", ValueReference.of("foobar"));
    }

    @Test
    @MongoDBFixtures("OutputFacadeTest.json")
    public void exportNativeEntity() throws NotFoundException {
        final Output output = outputService.load("5adf239e4b900a0fdb4e5197");

        final EntityDescriptor descriptor = EntityDescriptor.create(output.getId(), ModelTypes.OUTPUT_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportNativeEntity(output, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.OUTPUT_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final OutputEntity outputEntity = objectMapper.convertValue(entityV1.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo(ValueReference.of("STDOUT"));
        assertThat(outputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.outputs.LoggingOutput"));
        assertThat(outputEntity.configuration()).containsEntry("prefix", ValueReference.of("Writing message: "));
    }

    @Test
    public void createNativeEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.OUTPUT_V1)
                .data(objectMapper.convertValue(OutputEntity.create(
                        ValueReference.of("STDOUT"),
                        ValueReference.of("org.graylog2.outputs.LoggingOutput"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("prefix", "Writing message: "))
                ), JsonNode.class))
                .build();

        final NativeEntity<Output> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), Collections.emptyMap(), "username");

        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.OUTPUT_V1);
        assertThat(nativeEntity.entity().getTitle()).isEqualTo("STDOUT");
        assertThat(nativeEntity.entity().getType()).isEqualTo("org.graylog2.outputs.LoggingOutput");
        assertThat(nativeEntity.entity().getCreatorUserId()).isEqualTo("username");
        assertThat(nativeEntity.entity().getConfiguration()).containsEntry("prefix", "Writing message: ");
    }

    @Test
    @MongoDBFixtures("OutputFacadeTest.json")
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.OUTPUT_V1)
                .data(objectMapper.convertValue(OutputEntity.create(
                        ValueReference.of("STDOUT"),
                        ValueReference.of("org.graylog2.outputs.LoggingOutput"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("prefix", "Writing message: "))
                ), JsonNode.class))
                .build();
        final Optional<NativeEntity<Output>> existingOutput = facade.findExisting(entity, Collections.emptyMap());
        assertThat(existingOutput).isEmpty();
    }

    @Test
    @MongoDBFixtures("OutputFacadeTest.json")
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5adf239e4b900a0fdb4e5197"))
                .type(ModelTypes.OUTPUT_V1)
                .data(objectMapper.convertValue(OutputEntity.create(
                        ValueReference.of("STDOUT"),
                        ValueReference.of("org.graylog2.outputs.LoggingOutput"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("prefix", "Writing message: "))
                ), JsonNode.class))
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }

    @Test
    @MongoDBFixtures("OutputFacadeTest.json")
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf239e4b900a0fdb4e5197", ModelTypes.OUTPUT_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    @MongoDBFixtures("OutputFacadeTest.json")
    public void delete() throws NotFoundException {
        final Output output = outputService.load("5adf239e4b900a0fdb4e5197");
        assertThat(outputService.count()).isEqualTo(1L);
        facade.delete(output);
        assertThat(outputService.count()).isEqualTo(0L);
        assertThatThrownBy(() -> outputService.load("5adf239e4b900a0fdb4e5197"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> configuration = ImmutableMap.of();
        final OutputImpl output = OutputImpl.create(
                "01234567890",
                "Output Title",
                "org.graylog2.output.SomeOutputClass",
                "admin",
                configuration,
                new Date(0L),
                null
        );
        final EntityExcerpt excerpt = facade.createExcerpt(output);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(output.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.OUTPUT_V1);
        assertThat(excerpt.title()).isEqualTo(output.getTitle());
    }

    @Test
    @MongoDBFixtures("OutputFacadeTest.json")
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5adf239e4b900a0fdb4e5197"))
                .type(ModelTypes.OUTPUT_V1)
                .title("STDOUT")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @MongoDBFixtures("OutputFacadeTest.json")
    public void collectEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf239e4b900a0fdb4e5197", ModelTypes.OUTPUT_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> collectedEntity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.OUTPUT_V1);
        final OutputEntity outputEntity = objectMapper.convertValue(entity.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo(ValueReference.of("STDOUT"));
        assertThat(outputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.outputs.LoggingOutput"));
        assertThat(outputEntity.configuration()).isNotEmpty();
    }
}
