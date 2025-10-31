/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.security.entities;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.security.DBGrantService;
import org.graylog.security.shares.Grantee;
import org.graylog2.contentpacks.ContentPackEntityResolver;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class DefaultEntityDependencyResolver implements EntityDependencyResolver {
    private final ContentPackEntityResolver contentPackEntityResolver;
    private final GRNRegistry grnRegistry;
    private final GRNDescriptorService descriptorService;
    private final DBGrantService grantService;

    @Inject
    public DefaultEntityDependencyResolver(ContentPackEntityResolver contentPackEntityResolver,
                                           GRNRegistry grnRegistry,
                                           GRNDescriptorService descriptorService,
                                           DBGrantService grantService) {
        this.contentPackEntityResolver = contentPackEntityResolver;
        this.grnRegistry = grnRegistry;
        this.descriptorService = descriptorService;
        this.grantService = grantService;
    }

    @Override
    public ImmutableSet<EntityDescriptor> resolve(GRN entity) {
        return resolve(Set.of(entity));
    }

    @SuppressWarnings("UnstableApiUsage")
    protected ImmutableSet<EntityDescriptor> resolve(Collection<GRN> entities) {
        final var cpDescriptors = entities.stream().map(DefaultEntityDependencyResolver::toContentPackEntityDescriptor)
                .collect(Collectors.toUnmodifiableSet());
        final var dependencyGraph = contentPackEntityResolver.resolveEntityDependencyGraph(cpDescriptors);
        final var dependencies = dependencyGraph.nodes().stream()
                .filter(dependency -> !cpDescriptors.contains(dependency)) // Don't include the given entity in dependencies
                // Workaround to ignore outputs as dependencies of streams.
                // To view a stream with a custom output, a user does not need output permissions. Therefore, we
                // ignore outputs if they only appear as dependencies of streams.
                // TODO This is another reason why we shouldn't be using the content pack resolver ¯\_(ツ)_/¯
                .filter(dependency -> !ModelTypes.OUTPUT_V1.equals(dependency.type()) ||
                        dependencyGraph.predecessors(dependency).stream().anyMatch(
                                predecessor -> !ModelTypes.STREAM_V1.equals(predecessor.type()))
                )
                // TODO: Work around from using the content pack dependency resolver:
                //  We've added stream_title content pack entities in https://github.com/Graylog2/graylog2-server/pull/17089,
                //  but in this context we want to return the actual dependent Stream to add additional permissions to.
                .map(cpDescriptor -> ModelTypes.STREAM_REF_V1.equals(cpDescriptor.type())
                        ? org.graylog2.contentpacks.model.entities.EntityDescriptor.create(cpDescriptor.id(), ModelTypes.STREAM_V1)
                        : cpDescriptor)
                .map(cpDescriptor -> grnRegistry.newGRN(cpDescriptor.type().name(), cpDescriptor.id().id()))
                .collect(ImmutableSet.toImmutableSet());

        final ImmutableMap<GRN, Optional<String>> entityExcerpts = entityExcerpts();
        final Map<GRN, Set<GRN>> owners = grantService.getOwnersForTargets(dependencies);
        return dependencies.stream()
                .map(dependency -> descriptorFromGRN(dependency, entityExcerpts, owners))
                .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public EntityDescriptor descriptorFromGRN(GRN entity) {
        return descriptorFromGRN(entity, entityExcerpts(), grantService.getOwnersForTargets(ImmutableSet.of(entity)));
    }

    protected static org.graylog2.contentpacks.model.entities.EntityDescriptor toContentPackEntityDescriptor(GRN entity) {
        return org.graylog2.contentpacks.model.entities.EntityDescriptor.builder()
                .id(ModelId.of(entity.entity()))
                // TODO: This is a hack! Until we stop using the content-pack dependency resolver, we have to use a different version for dashboards here
                .type(ModelType.of(entity.type(), "dashboard".equals(entity.type()) ? "2" : "1")) // TODO: Any way of NOT hardcoding the version here?
                .build();
    }

    private EntityDescriptor descriptorFromGRN(GRN entity, ImmutableMap<GRN, Optional<String>> entityExcerpts, Map<GRN, Set<GRN>> targetOwners) {
        return EntityDescriptor.create(entity, getExcerptTitle(entity, entityExcerpts), getOwners(targetOwners.get(entity)));
    }

    private String getExcerptTitle(GRN entity, ImmutableMap<GRN, Optional<String>> entityExcerpts) {
        return entityExcerpts.get(entity) != null
                ? entityExcerpts.get(entity).orElse("unnamed dependency: <" + entity + ">")
                : "unknown dependency: <" + entity + ">";
    }

    private ImmutableMap<GRN, Optional<String>> entityExcerpts() {
        // TODO: Replace entity excerpt usage with GRNDescriptors once we implemented GRN descriptors for every entity
        return contentPackEntityResolver.listAllEntityExcerpts().stream()
                // TODO: Use the GRNRegistry instead of manually building a GRN. Requires all entity types to be in the registry.
                .collect(ImmutableMap.toImmutableMap(e -> GRNType.create(e.type().name()).newGRNBuilder().entity(e.id().id()).build(),
                        v -> Optional.ofNullable(v.title())));
    }

    private Set<Grantee> getOwners(@Nullable Set<GRN> owners) {
        return firstNonNull(owners, Collections.<GRN>emptySet()).stream()
                .map(descriptorService::getDescriptor)
                // TODO there is a duplicate in GranteeSharesService
                .map(descriptor -> {
                            if (descriptor.grn().equals(GRNRegistry.GLOBAL_USER_GRN)) {
                                return Grantee.createGlobal();
                            }
                            return Grantee.create(descriptor.grn(), descriptor.grn().type(), descriptor.title());
                        }
                )
                .collect(Collectors.toSet());
    }
}
