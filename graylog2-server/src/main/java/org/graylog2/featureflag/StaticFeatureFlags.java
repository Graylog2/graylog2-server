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
package org.graylog2.featureflag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.featureflag.FeatureFlagStringUtil.*;

class StaticFeatureFlags implements FeatureFlags {

    private static final Logger LOG = LoggerFactory.getLogger(StaticFeatureFlags.class);

    private static final String ON = "ON";
    private final Map<String, String> flags;
    private final Map<String, String> upperCaseFlags;

    public StaticFeatureFlags(Map<String, String> flags) {
        this.flags = Collections.unmodifiableMap(flags);
        upperCaseFlags = flags.entrySet().stream()
                .collect(Collectors.toMap(e -> toUpperCase(e.getKey()), Map.Entry::getValue));
    }

    @Override
    public Map<String, String> getAll() {
        return flags;
    }

    @Override
    public boolean isOn(String feature, boolean defaultValue) {
        String flag = upperCaseFlags.get(toUpperCase(feature));
        if (flag == null) {
            LOG.warn("Feature flag '{}' is not set. Fall back to default value '{}'", feature, defaultValue);
            return defaultValue;
        }
        return ON.equalsIgnoreCase(flag);
    }

}
