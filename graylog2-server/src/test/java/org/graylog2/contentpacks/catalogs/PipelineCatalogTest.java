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
package org.graylog2.contentpacks.catalogs;

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
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.codecs.PipelineCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.events.ClusterEventBus;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineCatalogTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private PipelineRuleParser pipelineRuleParser;
    private PipelineCatalog catalog;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongoRule.getMongoConnection();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());

        final PipelineService pipelineService = new MongoDbPipelineService(mongoConnection, mapperProvider, clusterEventBus);
        final PipelineStreamConnectionsService connectionsService = new MongoDbPipelineStreamConnectionsService(mongoConnection, mapperProvider, clusterEventBus);
        final PipelineCodec codec = new PipelineCodec(objectMapper, pipelineService, connectionsService);

        catalog = new PipelineCatalog(pipelineService, connectionsService, pipelineRuleParser, codec);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("Test"))
                .type(ModelTypes.PIPELINE)
                .title("Test")
                .build();

        final Set<EntityExcerpt> entityExcerpts = catalog.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = catalog.collectEntity(EntityDescriptor.create(ModelId.of("Test"), ModelTypes.PIPELINE));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("Test"));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE);
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entity.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("pipeline \"Test\"");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_pipelines.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
        final EntityDescriptor pipelineEntity = EntityDescriptor.create(ModelId.of("Test"), ModelTypes.PIPELINE);

        final Graph<EntityDescriptor> graph = catalog.resolve(pipelineEntity);

        final EntityDescriptor streamEntity = EntityDescriptor.create(ModelId.of("5adf23894b900a0fdb4e517d"), ModelTypes.STREAM);
        final EntityDescriptor ruleEntity1 = EntityDescriptor.create(ModelId.of("debug"), ModelTypes.PIPELINE_RULE);
        final EntityDescriptor ruleEntity2 = EntityDescriptor.create(ModelId.of("no-op"), ModelTypes.PIPELINE_RULE);
        assertThat(graph.nodes())
                .containsOnly(pipelineEntity, streamEntity, ruleEntity1, ruleEntity2);
    }
}
