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
package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Objects;

@AutoValue
public abstract class EntityScopeResponse {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_IS_MUTABLE = "is_mutable";

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_IS_MUTABLE)
    public abstract boolean mutable();

    @JsonCreator
    public static EntityScopeResponse create(@JsonProperty(FIELD_NAME) String name,
                                             @JsonProperty(FIELD_IS_MUTABLE) boolean mutable) {
        return new AutoValue_EntityScopeResponse(name, mutable);
    }

    public static EntityScopeResponse of(EntityScope scope) {

        Objects.requireNonNull(scope, "Entity Scope must not be null");

        return create(scope.getName(), scope.isMutable());
    }
}
