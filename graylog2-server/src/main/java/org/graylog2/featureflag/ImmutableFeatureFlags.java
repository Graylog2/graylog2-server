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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.shared.metrics.MetricUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.featureflag.FeatureFlagStringUtil.stringFormat;
import static org.graylog2.featureflag.FeatureFlagStringUtil.toUpperCase;

class ImmutableFeatureFlags implements FeatureFlags {

    private static final Logger LOG = LoggerFactory.getLogger(ImmutableFeatureFlags.class);
    private final Map<String, FeatureFlag> flags;

    public ImmutableFeatureFlags(Map<String, String> flags) {
        this.flags = flags.entrySet().stream()
                .collect(Collectors.toMap(e -> toUpperCase(e.getKey()), e -> new FeatureFlag(e.getKey(), e.getValue())));
    }

    @Override
    public Map<String, String> getAll() {
        return flags.values().stream().collect(Collectors.toMap(flag -> flag.name, flag -> flag.value));
    }

    @Override
    public boolean isOn(String feature, boolean defaultValue) {
        FeatureFlag flag = getFlag(feature);
        if (flag == null) {
            LOG.warn("Feature flag '{}' is not set. Fall back to default value '{}'", feature, defaultValue);
            return defaultValue;
        }
        return flag.isOn();
    }

    @Override
    public void incrementFeatureIsUsedCounter(String feature) {
        FeatureFlag flag = getFlag(feature);
        if (flag != null) {
            flag.incrementFeatureIsUsedCounter();
        } else {
            LOG.error("Feature flag {} don't exist! Could not update metric!", feature);
        }
    }

    private FeatureFlag getFlag(String feature) {
        return flags.get(toUpperCase(feature));
    }

    @Override
    public void initMetrics(MetricRegistry metricRegistry) {
        flags.values().forEach(flag -> flag.initMetrics(metricRegistry));
    }

    private static class FeatureFlag {
        private static final String ON = "ON";
        private Counter featureUsedCounter;
        private Counter featureFlagUsedCounter;
        private final String name;
        private final String value;
        private final String featureFlagUsedMetricName;
        private final String featureUsedMetricName;
        private final String featureFlagStateMetricName;

        FeatureFlag(String name, String state) {
            this.name = name;
            this.value = state;
            featureFlagUsedMetricName = stringFormat("org.graylog.featureflag.used.%s", name);
            featureUsedMetricName = stringFormat("org.graylog.feature.used.%s", name);
            featureFlagStateMetricName = stringFormat("org.graylog.featureflag.state.%s.%s", name, state);
        }

        boolean isOn() {
            if (featureFlagUsedCounter != null) {
                featureFlagUsedCounter.inc();
            }
            return ON.equalsIgnoreCase(value);
        }

        void incrementFeatureIsUsedCounter() {
            if (featureUsedCounter != null) {
                featureUsedCounter.inc();
            }
        }

        void initMetrics(MetricRegistry metricRegistry) {
            featureUsedCounter = metricRegistry.counter(featureUsedMetricName);
            featureFlagUsedCounter = metricRegistry.counter(featureFlagUsedMetricName);
            MetricUtils.getOrRegister(metricRegistry, featureFlagStateMetricName, (Gauge<Integer>) () -> 0);
        }
    }
}
