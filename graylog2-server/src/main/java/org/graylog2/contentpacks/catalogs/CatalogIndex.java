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
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.facades.UnsupportedEntityFacade;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.entities.EntitiesWithConstraints;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.utilities.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CatalogIndex {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogIndex.class);

    private final Map<ModelType, EntityFacade<?>> facades;

    @Inject
    public CatalogIndex(Map<ModelType, EntityFacade<?>> facades) {
        this.facades = facades;
    }

    public Set<EntityExcerpt> entityIndex() {
        final ImmutableSet.Builder<EntityExcerpt> entityIndexBuilder = ImmutableSet.builder();
        facades.values().forEach(catalog -> entityIndexBuilder.addAll(catalog.listEntityExcerpts()));
        return entityIndexBuilder.build();
    }

    public Set<EntityDescriptor> resolveEntities(Collection<EntityDescriptor> unresolvedEntities) {
        final MutableGraph<EntityDescriptor> dependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.insertion())
                .build();
        unresolvedEntities.forEach(dependencyGraph::addNode);

        final HashSet<EntityDescriptor> resolvedEntities = new HashSet<>();
        final MutableGraph<EntityDescriptor> finalDependencyGraph = resolveDependencyGraph(dependencyGraph, resolvedEntities);

        LOG.debug("Final dependency graph: {}", finalDependencyGraph);

        return finalDependencyGraph.nodes();
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

            final EntityFacade<?> facade = facades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);
            final Graph<EntityDescriptor> graph = facade.resolve(entityDescriptor);
            LOG.trace("Dependencies of entity {}: {}", entityDescriptor, graph);

            Graphs.merge(mutableGraph, graph);
            LOG.trace("New dependency graph: {}", mutableGraph);

            resolvedEntities.add(entityDescriptor);
            final Graph<EntityDescriptor> result = resolveDependencyGraph(mutableGraph, resolvedEntities);
            Graphs.merge(mutableGraph, result);
        }

        return mutableGraph;
    }

    public EntitiesWithConstraints collectEntities(Collection<EntityDescriptor> resolvedEntities) {
        final ImmutableSet.Builder<Entity> entities = ImmutableSet.builder();
        final ImmutableSet.Builder<Constraint> constraints = ImmutableSet.<Constraint>builder()
                .add(GraylogVersionConstraint.currentGraylogVersion());
        for (EntityDescriptor entityDescriptor : resolvedEntities) {
            final EntityFacade<?> facade = facades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);

            facade.exportEntity(entityDescriptor).ifPresent(entityWithConstraints -> {
                entities.add(entityWithConstraints.entity());
                constraints.addAll(entityWithConstraints.constraints());
            });
        }

        return EntitiesWithConstraints.create(entities.build(), constraints.build());
    }
}
