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
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.utilities.Graphs;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RootEntityFacade implements EntityFacade<Void> {
    public static final ModelType TYPE = ModelTypes.ROOT;

    @Override
    public NativeEntity<Void> createNativeEntity(Entity entity,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Object> nativeEntities,
                                                 String username) {
        throw new UnsupportedOperationException("Unsupported operation for root entity");
    }

    @Override
    public Optional<NativeEntity<Void>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return Optional.empty();
    }

    @Override
    public void delete(Void nativeEntity) {
    }

    @Override
    public EntityExcerpt createExcerpt(Void nativeEntity) {
        throw new UnsupportedOperationException("Unsupported operation for root entity");
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return Collections.emptySet();
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        return Optional.empty();
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        return Graphs.emptyDirectedGraph();
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        return Graphs.emptyDirectedGraph();
    }
}
