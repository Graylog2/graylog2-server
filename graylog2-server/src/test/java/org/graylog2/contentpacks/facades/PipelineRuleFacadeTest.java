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
import com.google.common.graph.Graph;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.exceptions.DivergingEntityConfigurationException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.contentpacks.model.entities.PipelineRuleEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PipelineRuleFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private RuleService ruleService;
    private PipelineRuleFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        ruleService = new MongoDbRuleService(
                mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);

        facade = new PipelineRuleFacade(objectMapper, ruleService);
    }

    @Test
    public void exportEntity() {
        final RuleDao pipelineRule = RuleDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")
                .build();
        final EntityWithConstraints entityWithConstraints = facade.exportNativeEntity(pipelineRule);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("title"));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("title"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("description"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("rule \"debug\"\n");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportNativeEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("debug", ModelTypes.PIPELINE_RULE_V1);
        final EntityWithConstraints entityWithConstraints = facade.exportEntity(descriptor).orElseThrow(AssertionError::new);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity.id()).isEqualTo(ModelId.of("debug"));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("debug"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("Debug"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("rule \"debug\"\n");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() throws NotFoundException {
        final RuleDao ruleDao = ruleService.loadByName("debug");
        assertThat(ruleService.loadAll()).hasSize(2);

        facade.delete(ruleDao);
        assertThatThrownBy(() -> ruleService.loadByName("debug"))
                .isInstanceOf(NotFoundException.class);
        assertThat(ruleService.loadAll()).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .data(objectMapper.convertValue(PipelineRuleEntity.create(
                        ValueReference.of("debug"),
                        ValueReference.of("Debug"),
                        ValueReference.of("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")), JsonNode.class))
                .build();
        final NativeEntity<RuleDao> existingRule = facade.findExisting(entity, Collections.emptyMap()).orElseThrow(AssertionError::new);

        assertThat(existingRule.descriptor().id()).isEqualTo(ModelId.of("5adf25034b900a0fdb4e5338"));
        assertThat(existingRule.descriptor().type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);
        assertThat(existingRule.entity().title()).isEqualTo("debug");
        assertThat(existingRule.entity().description()).isEqualTo("Debug");
        assertThat(existingRule.entity().source()).startsWith("rule \"debug\"\n");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findExistingWithDifferentSource() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .data(objectMapper.convertValue(PipelineRuleEntity.create(
                        ValueReference.of("debug"),
                        ValueReference.of("Debug"),
                        ValueReference.of("rule \"debug\"\nwhen\n  true\nthen\n\nend")), JsonNode.class))
                .build();
        assertThatThrownBy(() -> facade.findExisting(entity, Collections.emptyMap()))
                .isInstanceOf(DivergingEntityConfigurationException.class)
                .hasMessage("Different pipeline rule sources for pipeline rule with name \"debug\"");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .data(objectMapper.convertValue(PipelineRuleEntity.create(
                        ValueReference.of("debug"),
                        ValueReference.of("Debug"),
                        ValueReference.of("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")), JsonNode.class))
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("debug", ModelTypes.PIPELINE_RULE_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    public void createExcerpt() {
        final RuleDao pipelineRule = RuleDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(pipelineRule);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("title"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);
        assertThat(excerpt.title()).isEqualTo("title");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .title("debug")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("no-op"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .title("no-op")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.exportEntity(EntityDescriptor.create("debug", ModelTypes.PIPELINE_RULE_V1));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("debug"));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);
        final PipelineRuleEntity pipelineRuleEntity = objectMapper.convertValue(entity.data(), PipelineRuleEntity.class);
        assertThat(pipelineRuleEntity.title()).isEqualTo(ValueReference.of("debug"));
        assertThat(pipelineRuleEntity.description()).isEqualTo(ValueReference.of("Debug"));
        assertThat(pipelineRuleEntity.source().asString(Collections.emptyMap())).startsWith("rule \"debug\"\n");
    }
}
