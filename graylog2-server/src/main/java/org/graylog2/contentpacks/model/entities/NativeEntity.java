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
