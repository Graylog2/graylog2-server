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
package org.graylog.security.entities;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.security.shares.EntitySharePrepareResponse.MissingDependency;
import org.graylog2.utilities.GRN;
import org.graylog2.utilities.GRNRegistry;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public class EntityDependencyResolver {
    private final ContentPackService contentPackService;
    private final GRNRegistry grnRegistry;

    @Inject
    public EntityDependencyResolver(ContentPackService contentPackService, GRNRegistry grnRegistry) {
        this.contentPackService = contentPackService;
        this.grnRegistry = grnRegistry;
    }

    public ImmutableSet<MissingDependency> resolve(GRN entityGRN) {
        // TODO: Create a method in ContentPackService to only select some exerpts instead of loading all
        final ImmutableMap<GRN, String> entityExcerpts = contentPackService.listAllEntityExcerpts().stream()
                // TODO: Use the GRNRegistry instead of manually building a GRN. Requires all entity types to be in the registry.
                .collect(ImmutableMap.toImmutableMap(e -> GRN.builder().type(e.type().name()).entity(e.id().id()).permissionPrefix(e.type().name()+ ":").build() , EntityExcerpt::title));

        final Set<EntityDescriptor> descriptors = contentPackService.resolveEntities(Collections.singleton(EntityDescriptor.builder()
                .id(ModelId.of(entityGRN.entity()))
                .type(ModelType.of(entityGRN.type(), "2")) // TODO: Any way of NOT hardcoding the version here?
                .build()));

        // TODO: Resolve owners for the missing dependencies
        return descriptors.stream()
                .map(descriptor -> grnRegistry.newGRN(descriptor.type().name(), descriptor.id().id()))
                .filter(grn -> !entityGRN.equals(grn))
                .map(grn -> MissingDependency.create(grn.toString(), entityExcerpts.get(grn), ImmutableSet.of()))
                .collect(ImmutableSet.toImmutableSet());
    }
}
