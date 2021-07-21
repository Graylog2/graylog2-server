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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.graylog2.bootstrap.CmdLineTool.GRAYLOG_ENVIRONMENT_VAR_PREFIX;
import static org.graylog2.bootstrap.CmdLineTool.GRAYLOG_SYSTEM_PROP_PREFIX;
import static org.graylog2.featureflag.FeatureFlagStringUtil.startsWithIgnoreCase;
import static org.graylog2.featureflag.FeatureFlagStringUtil.stringFormat;
import static org.graylog2.featureflag.FeatureFlagStringUtil.toUpperCase;

class ImmutableFeatureFlagsCollector {

    private static final Logger LOG = LoggerFactory.getLogger(ImmutableFeatureFlagsCollector.class);

    private static final String GRAYLOG_FF_ENVIRONMENT_VAR_PREFIX = GRAYLOG_ENVIRONMENT_VAR_PREFIX + "FEATURE_";
    private static final String GRAYLOG_FF_SYSTEM_PROP_PREFIX = GRAYLOG_SYSTEM_PROP_PREFIX + "feature.";

    private Map<String, FeatureFlagValue> existingFlags = new HashMap<>();
    private final FeatureFlagsResources resources;
    private final String defaultPropertiesFile;
    private final String customPropertiesFile;

    public ImmutableFeatureFlagsCollector(FeatureFlagsResources resources, String defaultPropertiesFile, String customPropertiesFile) {
        this.resources = resources;
        this.defaultPropertiesFile = defaultPropertiesFile;
        this.customPropertiesFile = customPropertiesFile;
    }

    public Map<String, String> toMap() {
        existingFlags = new HashMap<>();
        addDefaultPropertiesFlags(defaultPropertiesFile);
        addCustomPropertiesFlags(customPropertiesFile);
        addSystemPropertiesFlags();
        addEnvironmentVariableFlags();
        printUsedFeatureFlags();
        return existingFlags.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value));
    }

    private void printUsedFeatureFlags() {
        LOG.info("Following feature flags are used: {}", existingFlags.entrySet().stream()
                .collect(Collectors.groupingBy(e -> e.getValue().resourceType)));
    }

    private void addDefaultPropertiesFlags(String file) {
        try {
            addFlags(resources.defaultProperties(file), "default properties file");
        } catch (IOException e) {
            throw new RuntimeException(
                    stringFormat("Unable to read default feature flags file %s!", file), e);
        }
    }

    private void addCustomPropertiesFlags(String file) {
        try {
            addFlags(resources.customProperties(file), "custom properties file");
        } catch (IOException e) {
            LOG.info("Unable to read custom feature flags file {}! Skipping...", file);
        }
    }

    private void addSystemPropertiesFlags() {
        addFlagsWithPrefix(GRAYLOG_FF_SYSTEM_PROP_PREFIX, resources.systemProperties(), "system properties");
    }

    private void addEnvironmentVariableFlags() {
        addFlagsWithPrefix(GRAYLOG_FF_ENVIRONMENT_VAR_PREFIX, resources.environmentVariables(), "environment variables");
    }

    private void addFlagsWithPrefix(String prefix, Map<String, String> newFlags, String resourceType) {
        addFlags(newFlags, resourceType, s -> startsWithIgnoreCase(s, prefix), s -> s.substring(prefix.length()));
    }

    private void addFlags(Map<String, String> newFlags, String resourceType) {
        addFlags(newFlags, resourceType, s -> true, Function.identity());
    }

    private void addFlags(Map<String, String> newFlags,
                          String resourceType,
                          Predicate<String> predicate,
                          Function<String, String> transform) {
        Multimap<String, String> possibleDuplicates = ArrayListMultimap.create();
        for (Map.Entry<String, String> entry : newFlags.entrySet()) {
            if (predicate.test(entry.getKey())) {
                String key = transform.apply(entry.getKey());
                addFlag(key, entry.getValue(), resourceType);
                possibleDuplicates.put(toUpperCase(key), key);
            }
        }
        checkForDuplicates(possibleDuplicates, resourceType);
    }

    private void checkForDuplicates(Multimap<String, String> possibleDuplicates, String source) {
        List<Collection<String>> duplicates = possibleDuplicates.asMap().values().stream()
                .filter(collection -> collection.size() > 1)
                .collect(toList());
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException(stringFormat("The following duplicate feature flags are found in %s: %s", source, duplicates));
        }
    }

    private void addFlag(String key, String value, String resourceType) {
        Optional<String> existingFlag = existingFlags.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(key))
                .findFirst();
        if (existingFlag.isPresent()) {
            add(existingFlag.get(), value, resourceType);
        } else {
            add(key, value, resourceType);
        }
    }

    private void add(String key, String value, String resourceType) {
        existingFlags.put(key, new FeatureFlagValue(value, resourceType));
    }

    private static class FeatureFlagValue {
        final String value;
        final String resourceType;

        private FeatureFlagValue(String value, String resourceType) {
            this.value = value;
            this.resourceType = resourceType;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
