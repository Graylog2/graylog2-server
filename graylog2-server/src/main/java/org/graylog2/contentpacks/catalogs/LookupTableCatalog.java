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

import org.graylog2.contentpacks.converters.LookupTableConverter;
import org.graylog2.contentpacks.converters.LookupTableExcerptConverter;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.lookup.db.DBLookupTableService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupTableCatalog implements EntityCatalog {
    private final DBLookupTableService lookupTableService;
    private final LookupTableExcerptConverter excerptConverter;
    private final LookupTableConverter converter;

    @Inject
    public LookupTableCatalog(DBLookupTableService lookupTableService,
                              LookupTableExcerptConverter excerptConverter,
                              LookupTableConverter converter) {
        this.lookupTableService = lookupTableService;
        this.excerptConverter = excerptConverter;
        this.converter = converter;
    }

    @Override
    public boolean supports(ModelType modelType) {
        return ModelTypes.LOOKUP_TABLE.equals(modelType);
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return lookupTableService.findAll().stream()
                .map(excerptConverter::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Entity> collectEntities(Collection<ModelId> modelIds) {
        final Set<String> idStrings = modelIds.stream()
                .map(ModelId::id)
                .collect(Collectors.toSet());
        return lookupTableService.findByNames(idStrings).stream()
                .map(converter::convert)
                .collect(Collectors.toSet());
    }
}
