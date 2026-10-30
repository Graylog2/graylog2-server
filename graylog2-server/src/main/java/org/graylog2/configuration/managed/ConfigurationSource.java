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
import com.github.joschi.jadconfig.Repository;
import com.github.joschi.jadconfig.info.ParameterInfo;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;

import java.util.Locale;

/**
 * Where the value of a configuration parameter came from.
 * <p>
 * JadConfig describes a source as a human readable string, which isn't stable enough for the user interface to make
 * decisions on, so the repository is mapped to one of these values instead.
 */
public enum ConfigurationSource {
    /**
     * No repository provided a value, so the default of the configuration bean is used.
     */
    DEFAULT,
    CONFIG_FILE,
    ENVIRONMENT,
    SYSTEM_PROPERTY,
    /**
     * The value has been set through the API and takes precedence over all other sources.
     */
    DATABASE,
    /**
     * The value came from a repository this mapping doesn't know, which shouldn't happen outside of tests.
     */
    UNKNOWN;

    @JsonCreator
    public static ConfigurationSource fromString(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * The source of the given parameter, as resolved while processing the configuration beans of this node.
     */
    public static ConfigurationSource of(ParameterInfo parameterInfo) {
        if (parameterInfo.isDefault() || parameterInfo.source() == null) {
            return DEFAULT;
        }
        return of(parameterInfo.source().repository());
    }

    private static ConfigurationSource of(Repository repository) {
        // Both the current and the legacy prefixes use the same repository types
        if (repository instanceof EnvironmentRepository) {
            return ENVIRONMENT;
        }
        if (repository instanceof SystemPropertiesRepository) {
            return SYSTEM_PROPERTY;
        }
        if (repository instanceof PropertiesRepository) {
            return CONFIG_FILE;
        }
        // The repository reading values set through the API is mapped here once stored values are applied on startup
        return UNKNOWN;
    }
}
