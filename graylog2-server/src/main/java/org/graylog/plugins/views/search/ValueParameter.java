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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

/**
 * Parameters describe variable inputs to queries.
 * <p>
 * They consist of a declaration and a binding. Parameters without a binding are called "free" or "unbound" parameters.
 * In order to execute a query all of its non-optional parameters must have a binding associated with them, i.e. be "bound".
 * <p>
 * The caller is expected to provide a {@link ValueParameter} object when binding previously declared parameters.
 * In that case the declaration elements do not need to be repeated, only its {@link ValueParameter#name() name} property.
 */
@AutoValue
@JsonTypeName(ValueParameter.TYPE_NAME)
@JsonDeserialize(builder = ValueParameter.Builder.class)
public abstract class ValueParameter implements Parameter {
    public static final String TYPE_NAME = "value-parameter-v1";

    public static Builder builder() {
        return new AutoValue_ValueParameter.Builder().type(TYPE_NAME).optional(false);
    }

    public static ValueParameter any(String name) {
        return builder().name(name).dataType("any").build();
    }

    public abstract Builder toBuilder();

    public ValueParameter applyExecutionState(ObjectMapper objectMapper, JsonNode state) {
        final JsonNode bindingState = state.path(name());

        if (bindingState.isMissingNode()) {
            return this;
        }

        final Binding binding = objectMapper.convertValue(bindingState, Binding.class);

        return toBuilder().binding(binding).build();
    }

    @AutoValue.Builder
    public abstract static class Builder implements Parameter.Builder<Builder> {

        public abstract ValueParameter build();

        // to fill the default values
        @JsonCreator
        public static Builder create() {
            return ValueParameter.builder().type(TYPE_NAME).optional(false);
        }
    }

}
