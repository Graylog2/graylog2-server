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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.info.ParameterInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Keeps information about all configuration parameters of this node, e.g. where their values came from.
 */
public class ConfigurationInfoService {

    private final Map<String, ParameterInfo> parameterInfos = new ConcurrentSkipListMap<>();

    public ConfigurationInfoService(Map<String, ParameterInfo> parameterInfos) {
        this.parameterInfos.putAll(parameterInfos);
    }

    /**
     * Returns the information of all configuration parameters, sorted by parameter name.
     * <p>
     * Values are as configured, the effective value used at runtime may differ.
     */
    public Map<String, ParameterInfo> getAll() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(parameterInfos));
    }

    /**
     * Returns the information of the configuration parameter with the given name.
     * <p>
     * Values are as configured, the effective value used at runtime may differ.
     */
    public Optional<ParameterInfo> get(String name) {
        return Optional.ofNullable(parameterInfos.get(name));
    }

    /**
     * Returns the information of the configuration parameters with the given names. Unknown names are skipped.
     * <p>
     * Values are as configured, the effective value used at runtime may differ.
     */
    public Map<String, ParameterInfo> get(Collection<String> names) {
        final Map<String, ParameterInfo> result = new LinkedHashMap<>();
        for (final String name : names) {
            final ParameterInfo info = parameterInfos.get(name);
            if (info != null) {
                result.put(name, info);
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
