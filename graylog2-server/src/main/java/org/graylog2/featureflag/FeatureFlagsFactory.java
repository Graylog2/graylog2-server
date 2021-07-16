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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.bootstrap.CmdLineTool.GRAYLOG_ENVIRONMENT_VAR_PREFIX;
import static org.graylog2.bootstrap.CmdLineTool.GRAYLOG_SYSTEM_PROP_PREFIX;
import static org.graylog2.featureflag.FeatureFlagStringUtil.startsWithIgnoreCase;

public class FeatureFlagsFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureFlagsFactory.class);
    private static final String GRAYLOG_FF_ENVIRONMENT_VAR_PREFIX = GRAYLOG_ENVIRONMENT_VAR_PREFIX + "FEATURE_";
    private static final String GRAYLOG_FF_SYSTEM_PROP_PREFIX = GRAYLOG_SYSTEM_PROP_PREFIX + "feature.";
    private static final String DEFAULT_PROPERTIES_FILE = "/org/graylog2/featureflag/feature-flag.config";

    public FeatureFlags createStaticFeatureFlags(String customPropertiesFile) {
        return createStaticFeatureFlags(new FeatureFlagsResources(), DEFAULT_PROPERTIES_FILE, customPropertiesFile);
    }

    public FeatureFlags createStaticFeatureFlags(FeatureFlagsResources resources, String defaultPropertiesFile, String customPropertiesFile) {
        Map<String, String> flags = new HashMap<>();
        addDefaultPropertiesFlags(flags, resources, defaultPropertiesFile);
        addCustomPropertiesFlags(flags, resources, customPropertiesFile);
        addSystemPropertiesFlags(flags, resources);
        addEnvironmentVariableFlags(flags, resources);
        LOG.info("Following feature flags are used: {}", flags);
        return new StaticFeatureFlags(flags);
    }

    private void addDefaultPropertiesFlags(Map<String, String> flags, FeatureFlagsResources resource, String file) {
        try {
            addFlags(flags, resource.defaultProperties(file));
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Unable to read default feature flags file %s!", file), e);
        }
    }

    private void addCustomPropertiesFlags(Map<String, String> flags, FeatureFlagsResources resource, String file) {
        try {
            addFlags(flags, resource.customProperties(file));
        } catch (Exception e) {
            LOG.info("Unable to read custom feature flags file {}! Skipping...", file);
        }
    }

    private void addSystemPropertiesFlags(Map<String, String> flags, FeatureFlagsResources resource) {
        addFlagsWithPrefix(GRAYLOG_FF_SYSTEM_PROP_PREFIX, flags, resource.systemProperties());
    }

    private void addEnvironmentVariableFlags(Map<String, String> builder, FeatureFlagsResources resource) {
        addFlagsWithPrefix(GRAYLOG_FF_ENVIRONMENT_VAR_PREFIX, builder, resource.environmentVariables());
    }

    private void addFlagsWithPrefix(String prefix,
                                    Map<String, String> flags,
                                    Map<String, String> resource) {
        for (Map.Entry<String, String> resourceEntry : resource.entrySet()) {
            if (startsWithIgnoreCase(resourceEntry.getKey(), prefix)) {
                String feature = resourceEntry.getKey().substring(prefix.length());
                addFlag(flags, feature, resourceEntry.getValue());
            }
        }
    }

    private void addFlag(Map<String, String> existingFlags, String key, String value) {
        Optional<String> existingFlag = existingFlags.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(key))
                .findFirst();
        if (existingFlag.isPresent()) {
            existingFlags.put(existingFlag.get(), value);
        } else {
            existingFlags.put(key, value);
        }
    }

    private void addFlags(Map<String, String> existingFlags, Map<String, String> newFlags) {
        newFlags.forEach((key, value) -> addFlag(existingFlags, key, value));
    }
}
