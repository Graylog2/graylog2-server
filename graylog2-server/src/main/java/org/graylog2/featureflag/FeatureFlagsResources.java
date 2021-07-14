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

import com.google.auto.value.AutoValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@AutoValue
abstract class FeatureFlagsResources {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureFlagsResources.class);

    static FeatureFlagsResources create(String customPropertiesFile) {
        return create(defaultPropertiesFeatureFlags(),
                customPropertiesFeatureFlags(customPropertiesFile),
                () -> toMap(System.getProperties()),
                System::getenv);
    }

    static FeatureFlagsResources create(FeatureFlagResource defaultPropertiesResource,
                                        FeatureFlagResource customPropertiesResource,
                                        FeatureFlagResource systemPropertiesResource,
                                        FeatureFlagResource environmentVariableResource) {
        return new AutoValue_FeatureFlagsResources(
                defaultPropertiesResource, customPropertiesResource, systemPropertiesResource, environmentVariableResource
        );
    }

    abstract FeatureFlagResource defaultPropertiesResource();

    abstract FeatureFlagResource customPropertiesResource();

    abstract FeatureFlagResource systemPropertiesResource();

    abstract FeatureFlagResource environmentVariableResource();


    public interface FeatureFlagResource {
        Map<String, String> flags() throws Exception;
    }

    public static FeatureFlagResource defaultPropertiesFeatureFlags() {
        return () -> {
            String path = "/org/graylog2/featureflag/feature-flag.config";
            InputStream resourceAsStream = FeatureFlagsResources.class.getResourceAsStream(path);
            Properties properties = new Properties();
            try {
                properties.load(resourceAsStream);
            } catch (IOException e) {
                LOG.error("Unable to read default feature flag file {}: ", path);
                throw e;
            }
            return toMap(properties);
        };
    }

    public static FeatureFlagResource customPropertiesFeatureFlags(String file) {
        return () -> {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(file));
            } catch (IOException e) {
                LOG.info("Unable to read custom feature flag file {}.", file);
                throw e;
            }
            return toMap(properties);
        };
    }

    private static Map<String, String> toMap(Properties properties) {
        return properties.stringPropertyNames().stream()
                .collect(Collectors.toMap(Function.identity(), properties::getProperty));
    }
}
