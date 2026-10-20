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

import java.util.List;

/**
 * The validation rules of a configuration parameter.
 * <p>
 * These are deliberately declarative instead of a {@link java.util.function.Predicate} or a JadConfig
 * {@link com.github.joschi.jadconfig.Validator}: a rule written in Java can only run in the process which declares it,
 * while these rules are serialized to the user interface and enforced by the API from a single definition.
 *
 * @param min           Smallest accepted value of an {@link ParameterType#INTEGER} parameter
 * @param max           Largest accepted value of an {@link ParameterType#INTEGER} parameter
 * @param pattern       Regular expression the raw value has to match
 * @param allowedValues The accepted values, or the accepted elements of a {@link ParameterType#LIST} parameter
 */
public record ParameterConstraints(@JsonProperty("min") @Nullable Long min,
                                   @JsonProperty("max") @Nullable Long max,
                                   @JsonProperty("pattern") @Nullable String pattern,
                                   @JsonProperty("allowed_values") List<String> allowedValues) {

    public ParameterConstraints {
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
    }

    public static ParameterConstraints none() {
        return new ParameterConstraints(null, null, null, List.of());
    }

    public static ParameterConstraints range(@Nullable Long min, @Nullable Long max) {
        return new ParameterConstraints(min, max, null, List.of());
    }

    public static ParameterConstraints pattern(String pattern) {
        return new ParameterConstraints(null, null, pattern, List.of());
    }

    public static ParameterConstraints allowedValues(String... allowedValues) {
        return new ParameterConstraints(null, null, null, List.of(allowedValues));
    }
}
