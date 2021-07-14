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

import org.graylog2.featureflag.FeatureFlagsResources.FeatureFlagResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.bootstrap.CmdLineTool.GRAYLOG_ENVIRONMENT_VAR_PREFIX;
import static org.graylog2.bootstrap.CmdLineTool.GRAYLOG_SYSTEM_PROP_PREFIX;
import static org.graylog2.featureflag.FeatureFlagStringUtil.*;

public class FeatureFlagsFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureFlagsFactory.class);
    private static final String GRAYLOG_FF_ENVIRONMENT_VAR_PREFIX = GRAYLOG_ENVIRONMENT_VAR_PREFIX + "FF_";
    private static final String GRAYLOG_FF_SYSTEM_PROP_PREFIX = GRAYLOG_SYSTEM_PROP_PREFIX + "ff.";

    public FeatureFlags createStaticFeatureFlags(String customPropertiesFile) {
        return createStaticFeatureFlags(FeatureFlagsResources.create(customPropertiesFile));
    }

    public FeatureFlags createStaticFeatureFlags(FeatureFlagsResources resources) {
        Map<String, String> flags = new HashMap<>();
        addDefaultPropertiesFlags(flags, resources.defaultPropertiesResource());
        addCustomPropertiesFlags(flags, resources.customPropertiesResource());
        addSystemPropertiesFlags(flags, resources.systemPropertiesResource());
        addEnvironmentVariableFlags(flags, resources.environmentVariableResource());
        LOG.info("Following feature flags are used: {}", flags);
        return new StaticFeatureFlags(flags);
    }

    private void addDefaultPropertiesFlags(Map<String, String> flags, FeatureFlagResource resource) {
        try {
            addFlags(flags, resource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read default properties feature flags!", e);
        }
    }

    private void addCustomPropertiesFlags(Map<String, String> flags, FeatureFlagResource resource) {
        try {
            addFlags(flags, resource);
        } catch (Exception e) {
            LOG.info("Custom properties feature flags could not be determined. Skipping...");
        }
    }

    private void addSystemPropertiesFlags(Map<String, String> flags, FeatureFlagResource resource) {
        try {
            addFlagsWithPrefix(GRAYLOG_FF_SYSTEM_PROP_PREFIX, flags, resource);
        } catch (Exception e) {
            LOG.info("System properties feature flags could not be determined. Skipping ...");
        }
    }

    private void addEnvironmentVariableFlags(Map<String, String> builder, FeatureFlagResource resource) {
        try {
            addFlagsWithPrefix(GRAYLOG_FF_ENVIRONMENT_VAR_PREFIX, builder, resource);
        } catch (Exception e) {
            LOG.info("Environment variable feature flags could not be determined. Skipping ...");
        }
    }

    private void addFlagsWithPrefix(String prefix,
                                    Map<String, String> flags,
                                    FeatureFlagResource resource) throws Exception {
        for (Map.Entry<String, String> resourceEntry : resource.flags().entrySet()) {
            if (startsWithIgnoreCase(resourceEntry.getKey(), prefix)) {
                String feature = resourceEntry.getKey().substring(prefix.length());
                addFlag(flags, feature, resourceEntry.getValue());
            }
        }
    }

    private void addFlag(Map<String, String> flags, String key, String value) {
        Optional<String> existingFlag = flags.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(key))
                .findFirst();
        if (existingFlag.isPresent()) {
            flags.put(existingFlag.get(), value);
        } else {
            flags.put(key, value);
        }
    }

    private void addFlags(Map<String, String> flags, FeatureFlagResource resource) throws Exception {
        resource.flags().forEach((key, value) -> addFlag(flags, key, value));
    }
}
