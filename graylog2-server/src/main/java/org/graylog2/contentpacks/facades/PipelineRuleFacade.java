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
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.PipelineRuleEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineRuleFacade implements EntityFacade<RuleDao> {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRuleFacade.class);

    public static final ModelType TYPE = ModelTypes.PIPELINE_RULE;

    private final ObjectMapper objectMapper;
    private final RuleService ruleService;

    @Inject
    public PipelineRuleFacade(ObjectMapper objectMapper, RuleService ruleService) {
        this.objectMapper = objectMapper;
        this.ruleService = ruleService;
    }

    @Override
    public EntityWithConstraints encode(RuleDao ruleDao) {
        final PipelineRuleEntity ruleEntity = PipelineRuleEntity.create(
                ValueReference.of(ruleDao.title()),
                ValueReference.of(ruleDao.description()),
                ValueReference.of(ruleDao.source()));
        final JsonNode data = objectMapper.convertValue(ruleEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(ruleDao.title()))
                .type(ModelTypes.PIPELINE_RULE)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public RuleDao decode(Entity entity, Map<String, ValueReference> parameters, String username) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private RuleDao decodeEntityV1(EntityV1 entity, Map<String, ValueReference> parameters) {
        final DateTime now = Tools.nowUTC();
        final PipelineRuleEntity ruleEntity = objectMapper.convertValue(entity.data(), PipelineRuleEntity.class);
        final ValueReference description = ruleEntity.description();
        final RuleDao ruleDao = RuleDao.builder()
                .title(ruleEntity.title().asString(parameters))
                .description(description == null ? null : description.asString(parameters))
                .source(ruleEntity.source().asString(parameters))
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

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return ruleService.loadAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final RuleDao ruleDao = ruleService.loadByName(modelId.id());
            return Optional.of(encode(ruleDao));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find pipeline rule {}", entityDescriptor, e);
            return Optional.empty();
        }
    }
}
