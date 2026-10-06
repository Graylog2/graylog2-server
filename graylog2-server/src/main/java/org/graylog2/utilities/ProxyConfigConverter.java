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
package org.graylog2.utilities;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

public class ProxyConfigConverter implements Converter<ProxyConfig> {
    private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");

    @Override
    public ProxyConfig convertFrom(String value) {
        // JadConfig calls us for any value that is present in the config repository, an empty one included.
        // A blank value means "no proxy configured" and must not fail the startup.
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            final ProxyConfig config = new ProxyConfig(URI.create(value));
            if (config.host() == null || config.host().isBlank()) {
                throw new ParameterException(f("Invalid proxy URI: \"%s\" (no host)", value));
            }
            if (config.scheme() == null || !SUPPORTED_SCHEMES.contains(config.scheme().toLowerCase(Locale.ROOT))) {
                throw new ParameterException(f("Invalid proxy URI: \"%s\" (scheme must be one of %s)",
                        value, SUPPORTED_SCHEMES));
            }
            return config;
        } catch (IllegalArgumentException e) {
            throw new ParameterException(f("Invalid proxy URI: \"%s\"", value), e);
        }
    }

    @Override
    public String convertTo(ProxyConfig value) {
        return value.uri().toString();
    }
}
