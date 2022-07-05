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
import org.graylog2.system.entityscope.EntityScope;

import static org.graylog2.system.entityscope.EntityScope.FIELD_IS_MODIFIABLE;
import static org.graylog2.system.entityscope.EntityScope.FIELD_NAME;

@AutoValue
public abstract class EntityScopeRequest {

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_IS_MODIFIABLE)
    public abstract boolean modifiable();

    public EntityScope toEntity() {
        return EntityScope.Builder
                .builder()
                .name(name())
                .modifiable(modifiable())
                .build();
    }

    @JsonCreator
    public static EntityScopeRequest create(@JsonProperty(FIELD_NAME) String name,
                                            @JsonProperty(FIELD_IS_MODIFIABLE) boolean isModifiable) {
        return new AutoValue_EntityScopeRequest(name, isModifiable);
    }
}
