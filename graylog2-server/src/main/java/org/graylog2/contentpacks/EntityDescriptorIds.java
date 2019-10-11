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

import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * This object maps {@link EntityDescriptor} objects to generated IDs that will be used as ID in
 * {@link org.graylog2.contentpacks.model.entities.Entity} objects.
 */
public class EntityDescriptorIds {
    private final ImmutableMap<EntityDescriptor, String> descriptorIds;

    public static EntityDescriptorIds empty() {
        return new EntityDescriptorIds(ImmutableMap.of());
    }

    public static EntityDescriptorIds of(EntityDescriptor... entityDescriptors) {
        return of(Arrays.asList(entityDescriptors));
    }

    public static EntityDescriptorIds of(Collection<EntityDescriptor> entityDescriptors) {
        final ImmutableMap<EntityDescriptor, String> descriptorIds = entityDescriptors.stream()
                .collect(ImmutableMap.toImmutableMap(Function.identity(), d -> UUID.randomUUID().toString()));

        return new EntityDescriptorIds(descriptorIds);
    }

    private EntityDescriptorIds(ImmutableMap<EntityDescriptor, String> descriptorIds) {
        this.descriptorIds = descriptorIds;
    }

    public Optional<String> get(final EntityDescriptor descriptor) {
        return Optional.ofNullable(descriptorIds.get(descriptor));
    }

    public Optional<String> get(final String id, final ModelType type) {
        return Optional.ofNullable(descriptorIds.get(EntityDescriptor.create(id, type)));
    }

    public String getOrThrow(EntityDescriptor descriptor) {
        return get(descriptor).orElseThrow(() -> new ContentPackException("Couldn't find entity " + descriptor.toString()));
    }

    public String getOrThrow(final String id, final ModelType type) {
        return get(id, type).orElseThrow(() -> new ContentPackException("Couldn't find entity " + id + "/" + type));
    }
}
