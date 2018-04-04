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

import org.graylog2.contentpacks.converters.GrokPatternConverter;
import org.graylog2.contentpacks.converters.GrokPatternExcerptConverter;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.grok.GrokPatternService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class GrokPatternCatalog implements EntityCatalog {
    private final GrokPatternService grokPatternService;
    private final GrokPatternExcerptConverter excerptConverter;
    private final GrokPatternConverter converter;

    @Inject
    public GrokPatternCatalog(GrokPatternService grokPatternService,
                              GrokPatternExcerptConverter excerptConverter,
                              GrokPatternConverter converter) {
        this.grokPatternService = grokPatternService;
        this.excerptConverter = excerptConverter;
        this.converter = converter;
    }

    @Override
    public boolean supports(ModelType modelType) {
        return ModelTypes.GROK_PATTERN.equals(modelType);
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return grokPatternService.loadAll().stream()
                .map(excerptConverter::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Entity> collectEntities(Collection<ModelId> modelIds) {
        final Set<String> idStrings = modelIds.stream()
                .map(ModelId::id)
                .collect(Collectors.toSet());
        return grokPatternService.bulkLoad(idStrings).stream()
                .map(converter::convert)
                .collect(Collectors.toSet());
    }
}
