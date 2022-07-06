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
package org.graylog2.rest.resources.entityscope;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.entityscope.EntityScope;

import javax.annotation.Nullable;

import static org.graylog2.entityscope.EntityScope.FIELD_DESCRIPTION;
import static org.graylog2.entityscope.EntityScope.FIELD_IS_MODIFIABLE;
import static org.graylog2.entityscope.EntityScope.FIELD_TITLE;

@AutoValue
public abstract class EntityScopeRequest {

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    public abstract String description();

    @JsonProperty(FIELD_IS_MODIFIABLE)
    public abstract boolean modifiable();

    public EntityScope toEntity() {
        return EntityScope.Builder
                .builder()
                .title(title())
                .description(description())
                .modifiable(modifiable())
                .build();
    }

    /**
     * Merge all the fields of this object with the {@link EntityScope} provided.
     *
     * <p>
     * A {@link EntityScopeRequest} may not have all the fields of a {@link EntityScope}.  The objective of this method is to
     * replace all the common fields with this object's fields.
     * </p>
     *
     * @param entity entity scope
     * @return entity scope
     */
    public EntityScope merge(EntityScope entity) {
        return entity.toBuilder()
                .title(title())
                .description(description())
                .modifiable(modifiable())
                .build();
    }

    @JsonCreator
    public static EntityScopeRequest create(@JsonProperty(FIELD_TITLE) String title,
                                            @JsonProperty(FIELD_DESCRIPTION) String description,
                                            @JsonProperty(FIELD_IS_MODIFIABLE) boolean isModifiable) {
        return new AutoValue_EntityScopeRequest(title, description, isModifiable);
    }
}
