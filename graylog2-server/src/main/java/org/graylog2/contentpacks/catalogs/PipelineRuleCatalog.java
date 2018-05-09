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

import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.contentpacks.codecs.PipelineRuleCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.database.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineRuleCatalog implements EntityCatalog {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRuleCatalog.class);

    public static final ModelType TYPE = ModelTypes.PIPELINE_RULE;

    private final RuleService ruleService;
    private final PipelineRuleCodec codec;

    @Inject
    public PipelineRuleCatalog(RuleService ruleService,
                               PipelineRuleCodec codec) {
        this.ruleService = ruleService;
        this.codec = codec;
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return ruleService.loadAll().stream()
                .map(codec::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final RuleDao ruleDao = ruleService.loadByName(modelId.id());
            return Optional.of(codec.encode(ruleDao));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find pipeline rule {}", entityDescriptor, e);
            return Optional.empty();
        }
    }
}
