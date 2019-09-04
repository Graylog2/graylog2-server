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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

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

    @Override
    public abstract String type();

    @JsonProperty
    public abstract String name();

    @Nullable
    @JsonProperty
    public abstract String title();

    @Nullable
    @JsonProperty
    public abstract String description();

    @JsonProperty("data_type")
    public abstract String dataType();

    @Nullable
    @JsonProperty("default_value")
    public abstract Object defaultValue();

    @JsonProperty
    public abstract boolean optional();

    @Nullable
    @JsonProperty
    public abstract Binding binding();

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
        @JsonProperty
        public abstract Builder name(String name);

        @JsonProperty
        public abstract Builder title(String title);

        @JsonProperty
        public abstract Builder description(String description);

        @JsonProperty
        public abstract Builder dataType(String dataType);

        @JsonProperty("default_value")
        public abstract Builder defaultValue(Object defaultValue);

        @JsonProperty
        public abstract Builder optional(boolean optional);

        @JsonProperty
        public abstract Builder binding(Binding binding);

        public abstract ValueParameter build();

        // to fill the default values
        @JsonCreator
        public static Builder create() {
            return ValueParameter.builder().type(TYPE_NAME).optional(false);
        }
    }

}
