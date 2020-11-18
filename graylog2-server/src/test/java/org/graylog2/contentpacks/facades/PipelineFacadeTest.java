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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.Graph;
import org.bson.types.ObjectId;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.buffers.processors.fakestreams.FakeStream;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private PipelineRuleParser pipelineRuleParser;
    private PipelineService pipelineService;
    private PipelineStreamConnectionsService connectionsService;
    @Mock
    private RuleService ruleService;
    @Mock
    private StreamService streamService;

    private PipelineFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());

        pipelineService = new MongoDbPipelineService(mongoConnection, mapperProvider, clusterEventBus);
        connectionsService = new MongoDbPipelineStreamConnectionsService(mongoConnection, mapperProvider, clusterEventBus);

        facade = new PipelineFacade(objectMapper, pipelineService, connectionsService, pipelineRuleParser, ruleService, streamService);
    }

    @Test
    public void exportEntity() {
        final PipelineDao pipeline = PipelineDao.builder()
                .id("pipeline-1234")
                .title("title")
                .description("description")
                .source("pipeline \"Test\"\nstage 0 match either\nrule \"debug\"\nend")
                .build();
        final PipelineConnections connections = PipelineConnections.create("id", "stream-1234", Collections.singleton("pipeline-1234"));
        connectionsService.save(connections);

        final EntityDescriptor descriptor = EntityDescriptor.create(pipeline.id(), ModelTypes.PIPELINE_V1);
        final EntityDescriptor streamDescriptor = EntityDescriptor.create("stream-1234", ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor, streamDescriptor);
        final Entity entity = facade.exportNativeEntity(pipeline, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("title"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("description"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("pipeline \"Test\"");
        assertThat(pipelineEntity.connectedStreams()).containsOnly(ValueReference.of(entityDescriptorIds.get(streamDescriptor).orElse(null)));
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines.json")
    public void exportNativeEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5a85c4854b900afd5d662be3", ModelTypes.PIPELINE_V1);
        final EntityDescriptor streamDescriptor = EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor, streamDescriptor);
        final Entity entity = facade.exportEntity(descriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("pipeline \"Test\"");
        assertThat(pipelineEntity.connectedStreams()).containsOnly(ValueReference.of(entityDescriptorIds.get(streamDescriptor).orElse(null)));
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines_default_stream.json")
    public void exportNativeEntityWithDefaultStream() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5a85c4854b900afd5d662be3", ModelTypes.PIPELINE_V1);
        final EntityDescriptor defaultStreamDescriptor = EntityDescriptor.create(Stream.DEFAULT_STREAM_ID, ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor, defaultStreamDescriptor);

        assertThat(entityDescriptorIds.get(defaultStreamDescriptor)).isEqualTo(Optional.of(Stream.DEFAULT_STREAM_ID));

        final Entity entity = facade.exportEntity(descriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("pipeline \"Test\"");
        assertThat(pipelineEntity.connectedStreams()).containsOnly(ValueReference.of(entityDescriptorIds.get(defaultStreamDescriptor).orElse(null)));
    }

    @Test
    public void createNativeEntity() throws NotFoundException {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.PIPELINE_V1)
                .data(objectMapper.convertValue(PipelineEntity.create(
                        ValueReference.of("Title"),
                        ValueReference.of("Description"),
                        ValueReference.of("pipeline \"Title\"\nstage 0 match either\nrule \"debug\"\nrule \"no-op\"\nend"),
                        Collections.singleton(ValueReference.of("5adf23894b900a0f00000001"))), JsonNode.class))
                .build();

        final EntityDescriptor streamDescriptor = EntityDescriptor.create("5adf23894b900a0f00000001", ModelTypes.STREAM_V1);
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("5adf23894b900a0f00000001");
        final Map<EntityDescriptor, Object> nativeEntities = Collections.singletonMap(streamDescriptor, stream);
        final NativeEntity<PipelineDao> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), nativeEntities, "username");

        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.PIPELINE_V1);
        assertThat(nativeEntity.entity().title()).isEqualTo("Title");
        assertThat(nativeEntity.entity().description()).isEqualTo("Description");
        assertThat(nativeEntity.entity().source()).startsWith("pipeline \"Title\"");

        assertThat(connectionsService.load("5adf23894b900a0f00000001").pipelineIds())
                .containsOnly(nativeEntity.entity().id());
    }

    @Test
    public void createNativeEntityWithDefaultStream() throws NotFoundException {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.PIPELINE_V1)
                .data(objectMapper.convertValue(PipelineEntity.create(
                        ValueReference.of("Title"),
                        ValueReference.of("Description"),
                        ValueReference.of("pipeline \"Title\"\nstage 0 match either\nrule \"debug\"\nrule \"no-op\"\nend"),
                        Collections.singleton(ValueReference.of(Stream.DEFAULT_STREAM_ID))), JsonNode.class))
                .build();

        final FakeStream fakeDefaultStream = new FakeStream("All message Fake") {
            @Override
            protected ObjectId getObjectId() {
                return new ObjectId(Stream.DEFAULT_STREAM_ID);
            }
        };
        when(streamService.load(Stream.DEFAULT_STREAM_ID)).thenReturn(fakeDefaultStream);

        final Map<EntityDescriptor, Object> nativeEntities = Collections.emptyMap();
        final NativeEntity<PipelineDao> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), nativeEntities, "username");

        assertThat(connectionsService.load(fakeDefaultStream.getId()).pipelineIds())
                .containsOnly(nativeEntity.entity().id());
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines.json")
    public void delete() throws NotFoundException {
        final PipelineDao pipelineDao = pipelineService.load("5a85c4854b900afd5d662be3");

        assertThat(pipelineService.loadAll()).hasSize(1);
        facade.delete(pipelineDao);
        assertThat(pipelineService.loadAll()).isEmpty();

        assertThatThrownBy(() -> pipelineService.load("5a85c4854b900afd5d662be3"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines.json")
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.PIPELINE_V1)
                .data(objectMapper.convertValue(PipelineEntity.create(
                        ValueReference.of("Title"),
                        ValueReference.of("Description"),
                        ValueReference.of("pipeline \"Title\"\nstage 0 match either\nrule \"debug\"\nrule \"no-op\"\nend"),
                        Collections.singleton(ValueReference.of("5adf23894b900a0f00000001"))), JsonNode.class))
                .build();

        final Optional<NativeEntity<PipelineDao>> existingEntity = facade.findExisting(entity, Collections.emptyMap());
        assertThat(existingEntity).isEmpty();
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines.json")
    public void resolveEntityDescriptor() {
        final Stage stage = Stage.builder()
                .stage(0)
                .matchAll(false)
                .ruleReferences(Collections.singletonList("no-op"))
                .build();
        final Pipeline pipeline = Pipeline.builder()
                .id("5a85c4854b900afd5d662be3")
                .name("Test")
                .stages(ImmutableSortedSet.of(stage))
                .build();
        when(pipelineRuleParser.parsePipeline("dummy", "pipeline \"Test\"\nstage 0 match either\nrule \"debug\"\nrule \"no-op\"\nend"))
                .thenReturn(pipeline);
        RuleDao ruleDao = RuleDao.builder()
                .id("2342353045938450345")
                .title("no-op")
                .source("rule \\\"debug\\\"\\nrule \\\"no-op\\\"\\nend\"")
                .build();

        when(ruleService.findByName("no-op")).thenReturn(Optional.of(ruleDao));
        final EntityDescriptor descriptor = EntityDescriptor.create("5a85c4854b900afd5d662be3", ModelTypes.PIPELINE_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(
                descriptor,
                EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1),
                EntityDescriptor.create("2342353045938450345", ModelTypes.PIPELINE_RULE_V1));
    }

    @Test
    public void createExcerpt() {
        final PipelineDao pipeline = PipelineDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("pipeline \"Test\"\nstage 0 match either\nrule \"debug\"\nend")
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(pipeline);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("id"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.PIPELINE_V1);
        assertThat(excerpt.title()).isEqualTo("title");
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines.json")
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5a85c4854b900afd5d662be3"))
                .type(ModelTypes.PIPELINE_V1)
                .title("Test")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines.json")
    public void collectEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5a85c4854b900afd5d662be3", ModelTypes.PIPELINE_V1);
        final EntityDescriptor streamDescriptor = EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor, streamDescriptor);
        final Optional<Entity> collectedEntity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_V1);
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entity.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("pipeline \"Test\"");
        assertThat(pipelineEntity.connectedStreams()).containsOnly(ValueReference.of(entityDescriptorIds.get(streamDescriptor).orElse(null)));
    }

    @Test
    @MongoDBFixtures("PipelineFacadeTest/pipelines.json")
    public void resolve() {
        final Stage stage = Stage.builder()
                .stage(0)
                .matchAll(false)
                .ruleReferences(ImmutableList.of("debug", "no-op"))
                .build();

        RuleDao ruleDao1 = RuleDao.builder()
                .id("2342353045938450345")
                .title("debug")
                .source("rule \\\"debug\\\"\\nrule \\\"no-op\\\"\\nend\"")
                .build();
        org.graylog.plugins.pipelineprocessor.ast.Rule rule1 = org.graylog.plugins.pipelineprocessor.ast.Rule.builder()
                .id("1")
                .name("debug")
                .when(mock(LogicalExpression.class))
                .then(Collections.emptyList())
                .build();

        RuleDao ruleDao2 = RuleDao.builder()
                .id("2342353045938450346")
                .title("no-op")
                .source("rule \\\"debug\\\"\\nrule \\\"no-op\\\"\\nend\"")
                .build();
        org.graylog.plugins.pipelineprocessor.ast.Rule rule2 = org.graylog.plugins.pipelineprocessor.ast.Rule.builder()
                .id("2")
                .name("no-op")
                .when(mock(LogicalExpression.class))
                .then(Collections.emptyList())
                .build();
        stage.setRules(ImmutableList.of(rule1, rule2));
        final Pipeline pipeline = Pipeline.builder()
                .id("5a85c4854b900afd5d662be3")
                .name("Test")
                .stages(ImmutableSortedSet.of(stage))
                .build();
        when(pipelineRuleParser.parsePipeline(eq("dummy"), anyString())).thenReturn(pipeline);
        when(ruleService.findByName("no-op")).thenReturn(Optional.of(ruleDao1));
        when(ruleService.findByName("debug")).thenReturn(Optional.of(ruleDao2));
        final EntityDescriptor pipelineEntity = EntityDescriptor.create("5a85c4854b900afd5d662be3", ModelTypes.PIPELINE_V1);

        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(pipelineEntity);

        final EntityDescriptor streamEntity = EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1);
        final EntityDescriptor ruleEntity1 = EntityDescriptor.create("2342353045938450345", ModelTypes.PIPELINE_RULE_V1);
        final EntityDescriptor ruleEntity2 = EntityDescriptor.create("2342353045938450346", ModelTypes.PIPELINE_RULE_V1);
        assertThat(graph.nodes())
                .containsOnly(pipelineEntity, streamEntity, ruleEntity1, ruleEntity2);
    }
}
