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

import com.codahale.metrics.MetricRegistry;

public class FeatureFlagsFactory {

    private static final String DEFAULT_PROPERTIES_FILE = "/org/graylog2/featureflag/feature-flag.config";

    public FeatureFlags createImmutableFeatureFlags(String customPropertiesFile, MetricRegistry metricRegistry) {
        return createImmutableFeatureFlags(
                new FeatureFlagsResources(),
                DEFAULT_PROPERTIES_FILE,
                customPropertiesFile,
                metricRegistry);
    }

    public FeatureFlags createImmutableFeatureFlags(FeatureFlagsResources resources,
                                                    String defaultPropertiesFile,
                                                    String customPropertiesFile,
                                                    MetricRegistry metricRegistry) {
        return new ImmutableFeatureFlags(new ImmutableFeatureFlagsCollector(
                resources, defaultPropertiesFile, customPropertiesFile).toMap(), metricRegistry);
    }
}
