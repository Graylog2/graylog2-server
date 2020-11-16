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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * Parameters describe variable inputs to queries.
 * <p>
 * They consist of a declaration and a binding. Parameters without a binding are called "free" or "unbound" parameters.
 * In order to execute a query all of its non-optional parameters must have a binding associated with them, i.e. be "bound".
 * <p>
 * The caller is expected to provide a {@link Parameter} object when binding previously declared parameters.
 * In that case the declaration elements do not need to be repeated, only its {@link Parameter#name() name} property.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = Parameter.TYPE_FIELD,
        visible = true,
        defaultImpl = ValueParameter.class)
public interface Parameter {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    @JsonProperty
    String name();

    @Nullable
    @JsonProperty
    String title();

    @Nullable
    @JsonProperty
    String description();

    @JsonProperty("data_type")
    String dataType();

    @Nullable
    @JsonProperty("default_value")
    Object defaultValue();

    @JsonProperty
    boolean optional();

    @Nullable
    @JsonProperty
    Binding binding();

    Parameter applyExecutionState(ObjectMapper objectMapper, JsonNode jsonNode);

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);

        @JsonProperty
        SELF name(String name);

        @JsonProperty
        SELF title(String title);

        @JsonProperty
        SELF description(String description);

        @JsonProperty("data_type")
        SELF dataType(String dataType);

        @JsonProperty("default_value")
        SELF defaultValue(Object defaultValue);

        @JsonProperty
        SELF optional(boolean optional);

        @JsonProperty
        SELF binding(Binding binding);
    }
    interface Factory<TYPE extends Parameter> {
        TYPE create(Parameter parameter);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = Binding.TYPE_FIELD,
            visible = true,
            defaultImpl = Binding.Fallback.class)
    interface Binding {
        String TYPE_FIELD = "type";

        @JsonProperty(TYPE_FIELD)
        String type();

        class Fallback implements Binding {
            @JsonProperty
            private String type;
            private Map<String, Object> props = Maps.newHashMap();

            @Override
            public String type() {
                return type;
            }

            @JsonAnySetter
            public void setProperties(String key, Object value) {
                props.put(key, value);
            }

            @JsonAnyGetter
            public Map<String, Object> getProperties() {
                return props;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Binding.Fallback fallback = (Binding.Fallback) o;
                return Objects.equals(type, fallback.type) &&
                        Objects.equals(props, fallback.props);
            }

            @Override
            public int hashCode() {
                return Objects.hash(type, props);
            }
        }
    }

    interface BindingHandler<B extends Binding, P extends Parameter> {
        // this method only exists because the compiler cannot treat `Binding` and `B extends Binding` as the same types
        // see SearchTypeHandler
        @SuppressWarnings("unchecked")
        default Object resolve(Binding binding, Parameter parameter, Map<String, QueryResult> results) {
            return doResolve((B) binding, (P) parameter, results);
        }

        Object doResolve(B binding, P parameter, Map<String, QueryResult> results);
    }
}
