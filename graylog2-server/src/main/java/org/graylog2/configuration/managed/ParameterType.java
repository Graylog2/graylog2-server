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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * The type of a configuration parameter value. Values are always stored and transferred as strings, this only tells
 * the user interface which input to show and how to interpret the raw value.
 */
public enum ParameterType {
    STRING,
    INTEGER,
    BOOLEAN,
    /**
     * A duration like {@code 30s} or {@code 5m}.
     */
    DURATION,
    /**
     * A data size like {@code 1g} or {@code 512m}.
     */
    SIZE,
    /**
     * A comma separated list of values.
     */
    LIST;

    @JsonCreator
    public static ParameterType fromString(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
