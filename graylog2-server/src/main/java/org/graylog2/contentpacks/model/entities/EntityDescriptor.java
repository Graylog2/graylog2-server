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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.Typed;

/**
 * The unique description of a (virtual) entity by ID and type.
 */
@AutoValue
@JsonDeserialize(builder = AutoValue_EntityDescriptor.Builder.class)
public abstract class EntityDescriptor implements Identified, Typed {
    public static EntityDescriptor create(ModelId id, ModelType type) {
        return builder()
                .id(id)
                .type(type)
                .build();
    }

    /**
     * Shortcut for {@link #create(ModelId, ModelType)}
     */
    public static EntityDescriptor create(String id, ModelType type) {
        return create(ModelId.of(id), type);
    }

    public static Builder builder() {
        return new AutoValue_EntityDescriptor.Builder();
    }

    @AutoValue.Builder
    public interface Builder extends IdBuilder<Builder>, TypeBuilder<Builder> {
        EntityDescriptor build();
    }
}
