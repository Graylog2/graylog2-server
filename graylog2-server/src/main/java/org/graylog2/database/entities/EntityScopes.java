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

import java.util.Map;

@AutoValue
public abstract class EntityScopes {

    private static final String FIELD_ENTITY_SCOPES = "entity_scopes";

    @JsonProperty(FIELD_ENTITY_SCOPES)
    public abstract Map<String, EntityScopeResponse> entityScopes();

    @JsonCreator
    public static EntityScopes create(@JsonProperty("entity_scopes") Map<String, EntityScopeResponse> entityScopes) {
        return new AutoValue_EntityScopes(entityScopes);
    }


}
