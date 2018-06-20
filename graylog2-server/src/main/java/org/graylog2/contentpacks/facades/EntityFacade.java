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
package org.graylog2.contentpacks.facades;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface EntityFacade<T> {
    EntityWithConstraints exportEntity(T nativeEntity);

    NativeEntity<T> createNativeEntity(Entity entity,
                                       Map<String, ValueReference> parameters,
                                       Map<EntityDescriptor, Object> nativeEntities,
                                       String username);

    default Optional<NativeEntity<T>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        return Optional.empty();
    }

    void delete(T nativeEntity);

    EntityExcerpt createExcerpt(T nativeEntity);

    Set<EntityExcerpt> listEntityExcerpts();

    Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor);

    default Graph<EntityDescriptor> resolve(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);
        return ImmutableGraph.copyOf(mutableGraph);
    }

    default Graph<Entity> resolve(Entity entity,
                                  Map<String, ValueReference> parameters,
                                  Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);
        return ImmutableGraph.copyOf(mutableGraph);
    }
}
