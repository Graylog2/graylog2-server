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
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CatalogIndex {
    private final Map<ModelType, EntityCatalog> catalogs;

    @Inject
    public CatalogIndex(Map<ModelType, EntityCatalog> catalogs) {
        this.catalogs = catalogs;
    }

    public Set<EntityExcerpt> entityIndex() {
        final ImmutableSet.Builder<EntityExcerpt> entityIndexBuilder = ImmutableSet.builder();
        catalogs.values().forEach(catalog -> entityIndexBuilder.addAll(catalog.listEntityExcerpts()));
        return entityIndexBuilder.build();
    }

    public Set<EntityDescriptor> resolveEntities(Collection<EntityDescriptor> unresolvedEntities) {
        final Set<EntityDescriptor> resolvableEntities = new HashSet<>(unresolvedEntities);
        final Set<EntityDescriptor> resolvedEntities = new HashSet<>();

        while (!resolvableEntities.isEmpty()) {
            for (EntityDescriptor entityDescriptor : resolvableEntities) {
                final EntityCatalog catalog = catalogs.get(entityDescriptor.type());
                final Set<EntityDescriptor> resolutionResult = catalog.resolve(entityDescriptor);
                resolvableEntities.addAll(resolutionResult);
                resolvedEntities.add(entityDescriptor);
            }

            resolvableEntities.removeAll(resolvedEntities);
        }

        return resolvedEntities;
    }

    public Set<Entity> collectEntities(Collection<EntityDescriptor> resolvedEntities) {
        final ImmutableSet.Builder<Entity> resultBuilder = ImmutableSet.builder();
        for (EntityDescriptor entityDescriptor : resolvedEntities) {
            final EntityCatalog catalog = catalogs.get(entityDescriptor.type());

            catalog.collectEntity(entityDescriptor).ifPresent(resultBuilder::add);
        }

        return resultBuilder.build();
    }
}
