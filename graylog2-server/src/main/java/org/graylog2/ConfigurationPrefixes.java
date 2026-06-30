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
package org.graylog2;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves the prefixes for reading node configuration from environment variables and system properties.
 * The defaults are {@code GRAYLOG_} for environment variables and {@code graylog.} for system properties.
 */
public final class ConfigurationPrefixes {

    /**
     * Environment variable that replaces the default configuration prefix. Read directly, so its own name is fixed.
     */
    public static final String ENV_OVERRIDE = "GRAYLOG_CONFIG_PREFIX";

    /**
     * System property equivalent of {@link #ENV_OVERRIDE}. The environment variable takes precedence.
     */
    public static final String SYSTEM_PROPERTY_OVERRIDE = "graylog.config.prefix";

    private static final String DEFAULT_ENV_PREFIX = "GRAYLOG_";
    private static final String DEFAULT_SYSTEM_PROPERTY_PREFIX = "graylog.";

    private ConfigurationPrefixes() {
    }

    /**
     * Environment-variable prefix: {@code GRAYLOG_}, or the override upper-cased with a trailing {@code _}.
     */
    public static String env() {
        return override().map(value -> value.toUpperCase(Locale.ROOT) + "_").orElse(DEFAULT_ENV_PREFIX);
    }

    /**
     * System-property prefix: {@code graylog.}, or the override lower-cased with a trailing {@code .}.
     */
    public static String sysProp() {
        return override().map(value -> value.toLowerCase(Locale.ROOT) + ".").orElse(DEFAULT_SYSTEM_PROPERTY_PREFIX);
    }

    private static Optional<String> override() {
        final String raw = StringUtils.firstNonBlank(
                System.getenv(ENV_OVERRIDE),
                System.getProperty(SYSTEM_PROPERTY_OVERRIDE)
        );
        final String root = StringUtils.stripEnd(StringUtils.trimToEmpty(raw), "._");
        return root.isEmpty() ? Optional.empty() : Optional.of(root);
    }
}
