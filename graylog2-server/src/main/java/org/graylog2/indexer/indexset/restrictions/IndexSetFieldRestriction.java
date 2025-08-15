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
package org.graylog2.indexer.indexset.restrictions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;

import static org.graylog2.indexer.indexset.restrictions.IndexSetFieldRestriction.TYPE_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = TYPE_FIELD, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ImmutableIndexSetField.class, name = ImmutableIndexSetField.TYPE_NAME),
})
public interface IndexSetFieldRestriction {
    String TYPE_FIELD = "type";
    String NAME_FIELD = "field_name";

    @NotNull
    @JsonProperty(TYPE_FIELD)
    String type();

    @NotNull
    @JsonProperty(NAME_FIELD)
    String fieldName();

    interface IndexSetFieldRestrictionBuilder<T> {

        @JsonProperty(TYPE_FIELD)
        T type(@NotNull String fieldName);

        @JsonProperty(NAME_FIELD)
        T fieldName(@NotNull String fieldName);
    }
}
