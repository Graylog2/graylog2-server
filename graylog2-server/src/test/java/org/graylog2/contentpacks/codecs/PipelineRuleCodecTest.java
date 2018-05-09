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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineRuleCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private RuleService ruleService;
    private PipelineRuleCodec codec;

    @Before
    public void setUp() {
        codec = new PipelineRuleCodec(objectMapper, ruleService);
    }

    @Test
    public void encode() {
        final RuleDao pipelineRule = RuleDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")
                .build();
        final EntityWithConstraints entityWithConstraints = codec.encode(pipelineRule);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("title"));
        assertThat(entity.type()).isEqualTo(ModelType.of("pipeline_rule"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo("title");
        assertThat(pipelineEntity.description()).isEqualTo("description");
        assertThat(pipelineEntity.source()).startsWith("rule \"debug\"\n");
    }

    @Test
    public void createExcerpt() {
        final RuleDao pipelineRule = RuleDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")
                .build();
        final EntityExcerpt excerpt = codec.createExcerpt(pipelineRule);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("title"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("pipeline_rule"));
        assertThat(excerpt.title()).isEqualTo("title");
    }
}