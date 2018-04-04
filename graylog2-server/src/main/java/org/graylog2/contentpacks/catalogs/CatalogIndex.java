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

import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;

import javax.inject.Inject;
import java.util.Set;

public class CatalogIndex {
    private final Set<EntityCatalog> catalogs;

    @Inject
    public CatalogIndex(Set<EntityCatalog> catalogs) {
        this.catalogs = catalogs;
    }

    public Set<EntityExcerpt> entityIndex() {
        final ImmutableSet.Builder<EntityExcerpt> entityIndexBuilder = ImmutableSet.builder();
        catalogs.forEach(catalog -> entityIndexBuilder.addAll(catalog.listEntityExcerpts()));
        return entityIndexBuilder.build();
    }
}
