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
package org.graylog2.configuration.managed;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The validation rules of a configuration parameter.
 * <p>
 * These are deliberately declarative instead of a {@link java.util.function.Predicate} or a JadConfig
 * {@link com.github.joschi.jadconfig.Validator}: a rule written in Java can only run in the process which declares it,
 * while these rules are serialized to the user interface and enforced by the API from a single definition.
 *
 * @param min     Smallest accepted value of an {@link ParameterType#INTEGER} parameter
 * @param max     Largest accepted value of an {@link ParameterType#INTEGER} parameter
 * @param pattern Regular expression the raw value has to match. The user interface applies it as well, so it has to
 *                stay portable between Java and JavaScript: no inline flags such as {@code (?i)}.
 * @param values  The values to choose from, mapped to the label the user interface shows, like the values of an input
 *                {@link org.graylog2.plugin.configuration.fields.DropdownField}. Empty if the value isn't picked from
 *                a list. Iteration order is the order the user interface offers them in.
 */
public record ParameterConstraints(@JsonProperty("min") @Nullable Long min,
                                   @JsonProperty("max") @Nullable Long max,
                                   @JsonProperty("pattern") @Nullable String pattern,
                                   @JsonProperty("values") Map<String, String> values) {

    public ParameterConstraints {
        // Map#copyOf doesn't keep the iteration order, which decides the order of the options
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static ParameterConstraints none() {
        return new ParameterConstraints(null, null, null, Map.of());
    }

    public static ParameterConstraints range(@Nullable Long min, @Nullable Long max) {
        return new ParameterConstraints(min, max, null, Map.of());
    }

    public static ParameterConstraints pattern(String pattern) {
        return new ParameterConstraints(null, null, pattern, Map.of());
    }

    public static ParameterConstraints values(Map<String, String> values) {
        return new ParameterConstraints(null, null, null, values);
    }
}
