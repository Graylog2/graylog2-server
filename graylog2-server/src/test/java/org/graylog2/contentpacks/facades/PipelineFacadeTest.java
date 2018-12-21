/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.Graph;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
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
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
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

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private PipelineRuleParser pipelineRuleParser;
    private PipelineService pipelineService;
    private PipelineStreamConnectionsService connectionsService;

    private PipelineFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongoRule.getMongoConnection();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());

        pipelineService = new MongoDbPipelineService(mongoConnection, mapperProvider, clusterEventBus);
        connectionsService = new MongoDbPipelineStreamConnectionsService(mongoConnection, mapperProvider, clusterEventBus);

        facade = new PipelineFacade(objectMapper, pipelineService, connectionsService, pipelineRuleParser);
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
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() throws NotFoundException {
        final PipelineDao pipelineDao = pipelineService.load("5a85c4854b900afd5d662be3");

        assertThat(pipelineService.loadAll()).hasSize(1);
        facade.delete(pipelineDao);
        assertThat(pipelineService.loadAll()).isEmpty();

        assertThatThrownBy(() -> pipelineService.load("5a85c4854b900afd5d662be3"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntityDescriptor() {
        final Stage stage = Stage.builder()
                .stage(0)
                .matchAll(false)
                .ruleReferences(Collections.singletonList("no-op"))
                .build();
        final Pipeline pipeline = Pipeline.builder()
                .id("id")
                .name("Test")
                .stages(ImmutableSortedSet.of(stage))
                .build();
        when(pipelineRuleParser.parsePipeline("dummy", "pipeline \"Test\"\nstage 0 match either\nrule \"debug\"\nrule \"no-op\"\nend"))
                .thenReturn(pipeline);
        final EntityDescriptor descriptor = EntityDescriptor.create("Test", ModelTypes.PIPELINE_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(
                descriptor,
                EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1),
                EntityDescriptor.create("no-op", ModelTypes.PIPELINE_RULE_V1));
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
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
    public void resolve() {
        final Stage stage = Stage.builder()
                .stage(0)
                .matchAll(false)
                .ruleReferences(ImmutableList.of("debug", "no-op"))
                .build();
        org.graylog.plugins.pipelineprocessor.ast.Rule rule1 = org.graylog.plugins.pipelineprocessor.ast.Rule.builder()
                .id("1")
                .name("debug")
                .when(mock(LogicalExpression.class))
                .then(Collections.emptyList())
                .build();
        org.graylog.plugins.pipelineprocessor.ast.Rule rule2 = org.graylog.plugins.pipelineprocessor.ast.Rule.builder()
                .id("2")
                .name("no-op")
                .when(mock(LogicalExpression.class))
                .then(Collections.emptyList())
                .build();
        stage.setRules(ImmutableList.of(rule1, rule2));
        final Pipeline pipeline = Pipeline.builder()
                .name("Test")
                .stages(ImmutableSortedSet.of(stage))
                .build();
        when(pipelineRuleParser.parsePipeline(eq("dummy"), anyString())).thenReturn(pipeline);
        final EntityDescriptor pipelineEntity = EntityDescriptor.create("Test", ModelTypes.PIPELINE_V1);

        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(pipelineEntity);

        final EntityDescriptor streamEntity = EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1);
        final EntityDescriptor ruleEntity1 = EntityDescriptor.create("debug", ModelTypes.PIPELINE_RULE_V1);
        final EntityDescriptor ruleEntity2 = EntityDescriptor.create("no-op", ModelTypes.PIPELINE_RULE_V1);
        assertThat(graph.nodes())
                .containsOnly(pipelineEntity, streamEntity, ruleEntity1, ruleEntity2);
    }
}
