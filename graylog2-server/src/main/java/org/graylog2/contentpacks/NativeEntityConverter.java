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
package org.graylog2.contentpacks;

import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

public interface NativeEntityConverter<T> {
    T toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities);
    default void resolveForInstallation(EntityV1 entity,
                                        Map<String, ValueReference> parameters,
                                        Map<EntityDescriptor, Entity> entities,
                                        MutableGraph<Entity> graph) {

    }
}
