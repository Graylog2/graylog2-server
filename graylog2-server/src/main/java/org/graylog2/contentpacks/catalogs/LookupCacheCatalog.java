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

import org.graylog2.contentpacks.converters.LookupCacheConverter;
import org.graylog2.contentpacks.converters.LookupCacheExcerptConverter;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.lookup.db.DBCacheService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupCacheCatalog implements EntityCatalog {
    private final DBCacheService cacheService;
    private final LookupCacheExcerptConverter excerptConverter;
    private final LookupCacheConverter converter;

    @Inject
    public LookupCacheCatalog(DBCacheService cacheService,
                              LookupCacheExcerptConverter excerptConverter,
                              LookupCacheConverter converter) {
        this.cacheService = cacheService;
        this.excerptConverter = excerptConverter;
        this.converter = converter;
    }

    @Override
    public boolean supports(ModelType modelType) {
        return ModelTypes.LOOKUP_CACHE.equals(modelType);
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return cacheService.findAll().stream()
                .map(excerptConverter::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Entity> collectEntities(Collection<ModelId> modelIds) {
        final Set<String> idStrings = modelIds.stream()
                .map(ModelId::id)
                .collect(Collectors.toSet());
        return cacheService.findByIds(idStrings).stream()
                .map(converter::convert)
                .collect(Collectors.toSet());
    }
}
