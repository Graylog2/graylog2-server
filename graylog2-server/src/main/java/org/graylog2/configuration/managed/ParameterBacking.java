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
 * Where a configuration parameter gets its value from, apart from the database.
 */
public enum ParameterBacking {
    /**
     * The parameter is declared in a configuration bean, so it can also be set in the configuration file, by an
     * environment variable or by a system property.
     */
    CONFIG_FILE,
    /**
     * The parameter only exists at runtime and is never read from a configuration bean, e.g. an OpenSearch cluster
     * setting.
     */
    RUNTIME;

    @JsonCreator
    public static ParameterBacking fromString(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
