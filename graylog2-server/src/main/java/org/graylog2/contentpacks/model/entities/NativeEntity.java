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
package org.graylog2.contentpacks.model.entities;

import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;

@AutoValue
public abstract class NativeEntity<T> {
    public abstract NativeEntityDescriptor descriptor();

    public abstract T entity();

    public static <T> NativeEntity<T> create(NativeEntityDescriptor entityDescriptor, T entity) {
        return new AutoValue_NativeEntity<>(entityDescriptor, entity);
    }

    /**
     * Shortcut for {@link #create(NativeEntityDescriptor, Object)}
     */
    public static <T> NativeEntity<T> create(String entityId, String nativeId, ModelType type, String title, boolean foundOnSystem, T entity) {
        return create(NativeEntityDescriptor.create(entityId, nativeId, type, title, foundOnSystem), entity);
    }

    public static <T> NativeEntity<T> create(ModelId entityId, String nativeId, ModelType type, String title, boolean foundOnSystem, T entity) {
        return create(NativeEntityDescriptor.create(entityId, nativeId, type, title, foundOnSystem), entity);
    }

    public static <T> NativeEntity<T> create(ModelId entityId, String nativeId, ModelType type, String title, T entity) {
        return create(NativeEntityDescriptor.create(entityId, nativeId, type, title, false), entity);
    }
}
