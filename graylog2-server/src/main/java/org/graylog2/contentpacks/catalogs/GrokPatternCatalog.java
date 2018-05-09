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

import org.graylog2.contentpacks.codecs.GrokPatternCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.database.NotFoundException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GrokPatternCatalog implements EntityCatalog {
    private static final Logger LOG = LoggerFactory.getLogger(GrokPatternCatalog.class);

    public static final ModelType TYPE = ModelTypes.GROK_PATTERN;

    private final GrokPatternService grokPatternService;
    private final GrokPatternCodec codec;

    @Inject
    public GrokPatternCatalog(GrokPatternService grokPatternService,
                              GrokPatternCodec codec) {
        this.grokPatternService = grokPatternService;
        this.codec = codec;
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return grokPatternService.loadAll().stream()
                .map(codec::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final GrokPattern grokPattern = grokPatternService.load(modelId.id());
            return Optional.of(codec.encode(grokPattern));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find grok pattern {}", entityDescriptor, e);
            return Optional.empty();
        }
    }
}
