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
package org.graylog2.contentpacks.model.constraints;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = Constraint.FIELD_META_TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GraylogVersionConstraint.class, name = GraylogVersionConstraint.TYPE_NAME),
        @JsonSubTypes.Type(value = PluginVersionConstraint.class, name = PluginVersionConstraint.TYPE_NAME)
})
public interface Constraint {
    String FIELD_META_TYPE = "type";

    @JsonProperty(FIELD_META_TYPE)
    String type();

    interface ConstraintBuilder<SELF> {
        @JsonProperty(FIELD_META_TYPE)
        SELF type(String type);
    }
}
