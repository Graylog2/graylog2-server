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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.PipelineRuleEntity;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import javax.inject.Inject;

public class PipelineRuleCodec implements EntityCodec<RuleDao> {
    private final ObjectMapper objectMapper;
    private final RuleService ruleService;

    @Inject
    public PipelineRuleCodec(ObjectMapper objectMapper, RuleService ruleService) {
        this.objectMapper = objectMapper;
        this.ruleService = ruleService;
    }

    @Override
    public EntityWithConstraints encode(RuleDao ruleDao) {
        final PipelineRuleEntity ruleEntity = PipelineRuleEntity.create(
                ruleDao.title(),
                ruleDao.description(),
                ruleDao.source());
        final JsonNode data = objectMapper.convertValue(ruleEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(ruleDao.title()))
                .type(ModelTypes.PIPELINE_RULE)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public RuleDao decode(Entity entity) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private RuleDao decodeEntityV1(EntityV1 entity) {
        final DateTime now = Tools.nowUTC();
        final PipelineRuleEntity ruleEntity = objectMapper.convertValue(entity.data(), PipelineRuleEntity.class);
        final RuleDao ruleDao = RuleDao.builder()
                .title(ruleEntity.title())
                .description(ruleEntity.description())
                .source(ruleEntity.source())
                .createdAt(now)
                .modifiedAt(now)
                .build();

        return ruleService.save(ruleDao);
    }

    @Override
    public EntityExcerpt createExcerpt(RuleDao ruleDao) {
        return EntityExcerpt.builder()
                .id(ModelId.of(ruleDao.title()))
                .type(ModelTypes.PIPELINE_RULE)
                .title(ruleDao.title())
                .build();
    }
}
