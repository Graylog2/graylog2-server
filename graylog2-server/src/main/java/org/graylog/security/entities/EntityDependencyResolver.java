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
import org.graylog.grn.GRNTypes;
import org.graylog.security.DBGrantService;
import org.graylog.security.shares.Grantee;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class EntityDependencyResolver {
    private final ContentPackService contentPackService;
    private final GRNRegistry grnRegistry;
    private final GRNDescriptorService descriptorService;
    private final DBGrantService grantService;
    // Some dependencies can be ignored.
    // E.g. To view a stream with a custom output, a user does not need output permissions
    private static final Map<GRNType, Set<ModelType>> IGNORED_DEPENDENCIES = ImmutableMap.<GRNType, Set<ModelType>>builder()
            .put(GRNTypes.SEARCH, ImmutableSet.of(ModelTypes.OUTPUT_V1))
            .put(GRNTypes.STREAM, ImmutableSet.of(ModelTypes.OUTPUT_V1))
            .put(GRNTypes.DASHBOARD, ImmutableSet.of(ModelTypes.OUTPUT_V1))
            .put(GRNTypes.EVENT_DEFINITION, ImmutableSet.of(ModelTypes.EVENT_PROCEDURE_V1))
            .build();

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

    public ImmutableSet<EntityDescriptor> resolve(GRN entity) {
        final ImmutableSet<GRN> dependencies = cpDescriptorfromGRN(entity).stream()
                .filter(dep -> {
                    // Filter dependencies that aren't needed for grants sharing
                    // TODO This is another reason why we shouldn't be using the content pack resolver ¯\_(ツ)_/¯
                    final Set<ModelType> ignoredDeps = IGNORED_DEPENDENCIES.getOrDefault(entity.grnType(), ImmutableSet.of());
                    return !ignoredDeps.contains(dep.type());
                })
                // TODO: Work around from using the content pack dependency resolver:
                //  We've added stream_title content pack entities in https://github.com/Graylog2/graylog2-server/pull/17089,
                //  but in this context we want to return the actual dependent Stream to add additional permissions to.
                .map(cpDescriptor -> ModelTypes.STREAM_REF_V1.equals(cpDescriptor.type())
                        ? org.graylog2.contentpacks.model.entities.EntityDescriptor.create(cpDescriptor.id(), ModelTypes.STREAM_V1)
                        : cpDescriptor)
                .map(cpDescriptor -> grnRegistry.newGRN(cpDescriptor.type().name(), cpDescriptor.id().id()))
                .filter(dependency -> !entity.equals(dependency)) // Don't include the given entity in dependencies
                .collect(ImmutableSet.toImmutableSet());

        return dependencies.stream()
                .flatMap(dependency -> descriptorFromGRN(dependency, entityExcerpts()).stream())
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableMap<GRN, Optional<String>> entityExcerpts() {
        // TODO: Replace entity excerpt usage with GRNDescriptors once we implemented GRN descriptors for every entity
        return contentPackService.listAllEntityExcerpts().stream()
                // TODO: Use the GRNRegistry instead of manually building a GRN. Requires all entity types to be in the registry.
                .collect(ImmutableMap.toImmutableMap(e -> GRNType.create(e.type().name(), e.type().name() + ":").newGRNBuilder().entity(e.id().id()).build(),
                        v -> Optional.ofNullable(v.title())));
    }

    public Optional<EntityDescriptor> descriptorFromGRN(GRN grn) {
        return descriptorFromGRN(grn, entityExcerpts()).stream().findFirst();
    }

    private Set<EntityDescriptor> descriptorFromGRN(GRN entity, ImmutableMap<GRN, Optional<String>> entityExcerpts) {
        return cpDescriptorfromGRN(entity).stream()
                .map(descriptor -> {
                    final Map<GRN, Set<GRN>> targetOwners = grantService.getOwnersForTargets(ImmutableSet.of(entity));
                    String title = entityExcerpts.get(entity) != null
                            ? entityExcerpts.get(entity).orElse("unnamed dependency: <" + entity + ">")
                            : "unknown dependency: <" + entity + ">";
                    return EntityDescriptor.create(entity, title, getOwners(targetOwners.get(entity)));
                })
                .collect(Collectors.toSet());
    }

    private Set<org.graylog2.contentpacks.model.entities.EntityDescriptor> cpDescriptorfromGRN(GRN entity) {
        return contentPackService.resolveEntities(Collections.singleton(org.graylog2.contentpacks.model.entities.EntityDescriptor.builder()
                .id(ModelId.of(entity.entity()))
                // TODO: This is a hack! Until we stop using the content-pack dependency resolver, we have to use a different version for dashboards here
                .type(ModelType.of(entity.type(), "dashboard".equals(entity.type()) ? "2" : "1")) // TODO: Any way of NOT hardcoding the version here?
                .build()));
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
