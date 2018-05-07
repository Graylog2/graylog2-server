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
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.codecs.PipelineRuleCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.PipelineRuleEntity;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class PipelineRuleCatalogTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private PipelineRuleCatalog catalog;

    @Before
    public void setUp() throws Exception {
        final RuleService ruleService = new MongoDbRuleService(mongoRule.getMongoConnection(), new MongoJackObjectMapperProvider(objectMapper));
        final PipelineRuleCodec codec = new PipelineRuleCodec(objectMapper, ruleService);

        catalog = new PipelineRuleCatalog(ruleService, codec);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE)
                .title("debug")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("no-op"))
                .type(ModelTypes.PIPELINE_RULE)
                .title("no-op")
                .build();

        final Set<EntityExcerpt> entityExcerpts = catalog.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/pipeline_processor_rules.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<Entity> collectedEntity = catalog.collectEntity(EntityDescriptor.create(ModelId.of("debug"), ModelTypes.PIPELINE_RULE));
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("debug"));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_RULE);
        final PipelineRuleEntity pipelineRuleEntity = objectMapper.convertValue(entity.data(), PipelineRuleEntity.class);
        assertThat(pipelineRuleEntity.title()).isEqualTo("debug");
        assertThat(pipelineRuleEntity.description()).isEqualTo("Debug");
        assertThat(pipelineRuleEntity.source()).startsWith("rule \"debug\"\n");
    }
}
