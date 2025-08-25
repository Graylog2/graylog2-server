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
package org.graylog2.contentpacks;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jakarta.inject.Inject;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.facades.UnsupportedEntityFacade;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.utilities.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ContentPackEntityResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackEntityResolver.class);
    private final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades;

    @Inject
    public ContentPackEntityResolver(Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades) {
        this.entityFacades = entityFacades;
    }

    public Set<EntityExcerpt> listAllEntityExcerpts() {
        final ImmutableSet.Builder<EntityExcerpt> entityIndexBuilder = ImmutableSet.builder();
        entityFacades.values().forEach(facade -> entityIndexBuilder.addAll(facade.listEntityExcerpts()));
        return entityIndexBuilder.build();
    }

    public Map<String, EntityExcerpt> getEntityExcerpts() {
        return listAllEntityExcerpts().stream().collect(Collectors.toMap(x -> x.id().id(), x -> x));
    }

    public ImmutableSet<Entity> collectEntities(Collection<EntityDescriptor> resolvedEntities) {
        // It's important to only compute the EntityDescriptor IDs once per #collectEntities call! Otherwise we
        // will get broken references between the entities.
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(resolvedEntities);

        final ImmutableSet.Builder<Entity> entities = ImmutableSet.builder();
        for (EntityDescriptor entityDescriptor : resolvedEntities) {
            if (EntityDescriptorIds.isSystemStreamDescriptor(entityDescriptor)) {
                continue;
            }
            final EntityWithExcerptFacade<?, ?> facade = entityFacades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);

            facade.exportEntity(entityDescriptor, entityDescriptorIds).ifPresent(entities::add);
        }

        return entities.build();
    }

    public Set<EntityDescriptor> resolveEntities(Collection<EntityDescriptor> unresolvedEntities) {
        return resolveEntityDependencyGraph(unresolvedEntities).nodes();
    }

    public Graph<EntityDescriptor> resolveEntityDependencyGraph(Collection<EntityDescriptor> unresolvedEntities) {
        final MutableGraph<EntityDescriptor> dependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.insertion())
                .build();
        unresolvedEntities.forEach(dependencyGraph::addNode);

        final HashSet<EntityDescriptor> resolvedEntities = new HashSet<>();
        final MutableGraph<EntityDescriptor> finalDependencyGraph = resolveDependencyGraph(dependencyGraph, resolvedEntities);

        LOG.debug("Final dependency graph: {}", finalDependencyGraph);

        return finalDependencyGraph;
    }

    private MutableGraph<EntityDescriptor> resolveDependencyGraph(Graph<EntityDescriptor> dependencyGraph, Set<EntityDescriptor> resolvedEntities) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.from(dependencyGraph).build();
        Graphs.merge(mutableGraph, dependencyGraph);

        for (EntityDescriptor entityDescriptor : dependencyGraph.nodes()) {
            LOG.debug("Resolving entity {}", entityDescriptor);
            if (resolvedEntities.contains(entityDescriptor)) {
                LOG.debug("Entity {} already resolved, skipping.", entityDescriptor);
                continue;
            }

            final EntityWithExcerptFacade<?, ?> facade = entityFacades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);
            final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(entityDescriptor);
            LOG.trace("Dependencies of entity {}: {}", entityDescriptor, graph);

            Graphs.merge(mutableGraph, graph);
            LOG.trace("New dependency graph: {}", mutableGraph);

            resolvedEntities.add(entityDescriptor);
            final Graph<EntityDescriptor> result = resolveDependencyGraph(mutableGraph, resolvedEntities);
            Graphs.merge(mutableGraph, result);
        }

        return mutableGraph;
    }
}
