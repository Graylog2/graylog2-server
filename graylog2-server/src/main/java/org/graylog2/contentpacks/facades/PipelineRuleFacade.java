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
import org.graylog2.contentpacks.exceptions.DivergingEntityConfigurationException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
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

    public static final ModelType TYPE = ModelTypes.PIPELINE_RULE_V1;

    private final ObjectMapper objectMapper;
    private final RuleService ruleService;

    @Inject
    public PipelineRuleFacade(ObjectMapper objectMapper, RuleService ruleService) {
        this.objectMapper = objectMapper;
        this.ruleService = ruleService;
    }

    @Override
    public EntityWithConstraints exportNativeEntity(RuleDao ruleDao) {
        final PipelineRuleEntity ruleEntity = PipelineRuleEntity.create(
                ValueReference.of(ruleDao.title()),
                ValueReference.of(ruleDao.description()),
                ValueReference.of(ruleDao.source()));
        final JsonNode data = objectMapper.convertValue(ruleEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(ruleDao.title()))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public NativeEntity<RuleDao> createNativeEntity(Entity entity,
                                                    Map<String, ValueReference> parameters,
                                                    Map<EntityDescriptor, Object> nativeEntities,
                                                    String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<RuleDao> decode(EntityV1 entity, Map<String, ValueReference> parameters) {
        final PipelineRuleEntity ruleEntity = objectMapper.convertValue(entity.data(), PipelineRuleEntity.class);
        final String title = ruleEntity.title().asString(parameters);
        final String source = ruleEntity.source().asString(parameters);

        final DateTime now = Tools.nowUTC();
        final ValueReference description = ruleEntity.description();
        final RuleDao ruleDao = RuleDao.builder()
                .title(title)
                .description(description == null ? null : description.asString(parameters))
                .source(source)
                .createdAt(now)
                .modifiedAt(now)
                .build();

        final RuleDao savedRuleDao = ruleService.save(ruleDao);
        return NativeEntity.create(entity.id(), savedRuleDao.id(), TYPE, savedRuleDao);
    }

    @Override
    public void delete(RuleDao nativeEntity) {
        ruleService.delete(nativeEntity.id());
    }

    @Override
    public Optional<NativeEntity<RuleDao>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        if (entity instanceof EntityV1) {
            return findExisting((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<RuleDao>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final PipelineRuleEntity ruleEntity = objectMapper.convertValue(entity.data(), PipelineRuleEntity.class);

        final String title = ruleEntity.title().asString(parameters);
        final String source = ruleEntity.source().asString(parameters);

        try {
            final RuleDao ruleDao = ruleService.loadByName(title);
            compareRuleSources(title, source, ruleDao.source());

            return Optional.of(NativeEntity.create(entity.id(), ruleDao.id(), TYPE, ruleDao));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private void compareRuleSources(String name, String expectedRuleSource, String actualRuleSource) {
        if (!actualRuleSource.equals(expectedRuleSource)) {
            LOG.debug("Expected source for rule \"{}\":\n{}\n\nActual source:\n{}", name, expectedRuleSource, actualRuleSource);
            throw new DivergingEntityConfigurationException("Different pipeline rule sources for pipeline rule with name \"" + name + "\"");
        }
    }

    @Override
    public EntityExcerpt createExcerpt(RuleDao ruleDao) {
        return EntityExcerpt.builder()
                .id(ModelId.of(ruleDao.title()))
                .type(ModelTypes.PIPELINE_RULE_V1)
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
    public Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final RuleDao ruleDao = ruleService.loadByName(modelId.id());
            return Optional.of(exportNativeEntity(ruleDao));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find pipeline rule {}", entityDescriptor, e);
            return Optional.empty();
        }
    }
}
