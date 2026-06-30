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
package org.graylog2.web.customization;

import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;


public class CustomizationConfig {
    private static final String DEFAULT_PRODUCT_NAME = "Graylog";

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

    private final Config config;
    private final Base64.Decoder base64Decoder;

    @Inject
    public CustomizationConfig(@Nullable Config config) {
        this.config = Optional.ofNullable(config).orElse(Config.empty());
        this.base64Decoder = Base64.getDecoder();
    }

    public String productName() {
        return config.productName().orElse(DEFAULT_PRODUCT_NAME);
    }

    public Optional<byte[]> favicon() {
        return config.favicon().map(base64Decoder::decode);
    }

    public static CustomizationConfig empty() {
        return new CustomizationConfig(Config.empty());
    }

    /**
     * {@code GRAYLOG_} by default, or the configured override upper-cased with a trailing {@code _}.
     */
    public static String environmentVariablePrefix() {
        return prefixOverride().map(value -> value.toUpperCase(Locale.ROOT) + "_").orElse(DEFAULT_ENV_PREFIX);
    }

    /**
     * {@code graylog.} by default, or the configured override lower-cased with a trailing {@code .}.
     */
    public static String systemPropertyPrefix() {
        return prefixOverride().map(value -> value.toLowerCase(Locale.ROOT) + ".").orElse(DEFAULT_SYSTEM_PROPERTY_PREFIX);
    }

    private static Optional<String> prefixOverride() {
        final String raw = StringUtils.firstNonBlank(
                System.getenv(ENV_OVERRIDE),
                System.getProperty(SYSTEM_PROPERTY_OVERRIDE)
        );
        final String root = StringUtils.stripEnd(StringUtils.trimToEmpty(raw), "._");
        return root.isEmpty() ? Optional.empty() : Optional.of(root);
    }
}
