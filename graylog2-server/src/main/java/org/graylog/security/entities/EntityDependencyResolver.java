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
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.security.DBGrantService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class EntityDependencyResolver {
    private final ContentPackService contentPackService;
    private final GRNRegistry grnRegistry;
    private final GRNDescriptorService descriptorService;
    private final DBGrantService grantService;

    @Inject
    public EntityDependencyResolver(ContentPackService contentPackService,
                                    GRNRegistry grnRegistry,
                                    GRNDescriptorService descriptorService,
                                    DBGrantService grantService) {
        this.contentPackService = contentPackService;
        this.grnRegistry = grnRegistry;
        this.descriptorService = descriptorService;
        this.grantService = grantService;
    }

    public ImmutableSet<EntityDependency> resolve(GRN entity) {
        // TODO: Replace entity excerpt usage with GRNDescriptors once we implemented GRN descriptors for every entity
        final ImmutableMap<GRN, String> entityExcerpts = contentPackService.listAllEntityExcerpts().stream()
                // TODO: Use the GRNRegistry instead of manually building a GRN. Requires all entity types to be in the registry.
                .collect(ImmutableMap.toImmutableMap(e -> GRNType.create(e.type().name(), e.type().name() + ":").newGRNBuilder().entity(e.id().id()).build(), EntityExcerpt::title));

        final Set<EntityDescriptor> descriptors = contentPackService.resolveEntities(Collections.singleton(EntityDescriptor.builder()
                .id(ModelId.of(entity.entity()))
                .type(ModelType.of(entity.type(), "2")) // TODO: Any way of NOT hardcoding the version here?
                .build()));

        final ImmutableSet<GRN> dependencies = descriptors.stream()
                .map(descriptor -> grnRegistry.newGRN(descriptor.type().name(), descriptor.id().id()))
                .filter(dependency -> !entity.equals(dependency)) // Don't include the given entity in dependencies
                .collect(ImmutableSet.toImmutableSet());

        final Map<GRN, Set<GRN>> targetOwners = grantService.getOwnersForTargets(dependencies);

        return dependencies.stream()
                .map(dependency -> EntityDependency.create(
                        dependency,
                        entityExcerpts.get(dependency),
                        getOwners(targetOwners.get(dependency))
                ))
                .collect(ImmutableSet.toImmutableSet());
    }

    private Set<EntityDependency.Owner> getOwners(@Nullable Set<GRN> owners) {
        return firstNonNull(owners, Collections.<GRN>emptySet()).stream()
                .map(descriptorService::getDescriptor)
                .map(descriptor -> EntityDependency.Owner.create(descriptor.grn(), descriptor.title()))
                .collect(Collectors.toSet());
    }
}
