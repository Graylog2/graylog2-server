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
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.entities.EntitiesWithConstraints;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CatalogIndex {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogIndex.class);

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
        final MutableGraph<EntityDescriptor> dependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.insertion())
                .build();
        unresolvedEntities.forEach(dependencyGraph::addNode);

        final HashSet<EntityDescriptor> resolvedEntities = new HashSet<>();
        resolveDependencyGraph(dependencyGraph, resolvedEntities);

        LOG.debug("Final dependency graph: {}", dependencyGraph);

        return dependencyGraph.nodes();
    }

    private void resolveDependencyGraph(MutableGraph<EntityDescriptor> dependencyGraph, Set<EntityDescriptor> resolvedEntities) {
        for (EntityDescriptor entityDescriptor : dependencyGraph.nodes()) {
            LOG.debug("Resolving entity {}", entityDescriptor);
            if (resolvedEntities.contains(entityDescriptor)) {
                LOG.debug("Entity {} already resolved, skipping.", entityDescriptor);
                continue;
            }

            final EntityCatalog catalog = catalogs.getOrDefault(entityDescriptor.type(), UnsupportedEntityCatalog.INSTANCE);
            final Graph<EntityDescriptor> graph = catalog.resolve(entityDescriptor);
            LOG.trace("Dependencies of entity {}: {}", entityDescriptor, graph);

            mergeGraphs(dependencyGraph, graph);
            LOG.trace("New dependency graph: {}", dependencyGraph);

            resolvedEntities.add(entityDescriptor);
            resolveDependencyGraph(dependencyGraph, resolvedEntities);
        }
    }

    private void mergeGraphs(MutableGraph<EntityDescriptor> g1, Graph<EntityDescriptor> g2) {
        LOG.trace("Merging {} with {}", g1, g2);
        g2.edges().forEach(edge -> g1.putEdge(edge.nodeU(), edge.nodeV()));
    }

    public EntitiesWithConstraints collectEntities(Collection<EntityDescriptor> resolvedEntities) {
        final ImmutableSet.Builder<Entity> entities = ImmutableSet.builder();
        final ImmutableSet.Builder<Constraint> constraints = ImmutableSet.<Constraint>builder()
                .add(GraylogVersionConstraint.currentGraylogVersion());
        for (EntityDescriptor entityDescriptor : resolvedEntities) {
            final EntityCatalog catalog = catalogs.getOrDefault(entityDescriptor.type(), UnsupportedEntityCatalog.INSTANCE);

            catalog.collectEntity(entityDescriptor).ifPresent(entityWithConstraints -> {
                entities.add(entityWithConstraints.entity());
                constraints.addAll(entityWithConstraints.constraints());
            });
        }

        return EntitiesWithConstraints.create(entities.build(), constraints.build());
    }
}
