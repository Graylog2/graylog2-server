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

    public ImmutableFeatureFlags(Map<String, String> flags, MetricRegistry metricRegistry) {
        this.flags = flags.entrySet().stream()
                .collect(Collectors.toMap(e -> toUpperCase(e.getKey()), e -> new FeatureFlag(e.getKey(), e.getValue(), metricRegistry)));
    }

    @Override
    public Map<String, String> getAll() {
        return flags.values().stream().collect(Collectors.toMap(flag -> flag.name, flag -> flag.value));
    }

    @Override
    public boolean isOn(String feature) {
        FeatureFlag flag = getFlag(feature);
        if (flag == null) {
            LOG.warn("Feature flag '{}' is not set. Fall back to default value 'false'", feature);
            return false;
        }
        return flag.isOn();
    }

    @Override
    public void incrementFeatureIsUsedCounter(String feature) {
        FeatureFlag flag = getFlag(feature);
        if (flag != null) {
            flag.incrementFeatureIsUsedCounter();
        } else {
            LOG.warn("Feature flag '{}' don't exist! Could not update metric!", feature);
        }
    }

    private FeatureFlag getFlag(String feature) {
        return flags.get(toUpperCase(feature));
    }

    private static class FeatureFlag {
        private static final String ON = "ON";
        private static final String VALID_METRIC_NAME_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*";
        private Counter featureUsedCounter;
        private Counter featureFlagUsedCounter;
        private final String name;
        private final String value;

        FeatureFlag(String name, String state, MetricRegistry metricRegistry) {
            this.name = name;
            this.value = state;
            if (validMetricName(name)) {
                initMetrics(name, state, metricRegistry);
            } else {
                LOG.warn("Metrics for feature flag '{}' can not be collected! Invalid characters.", name);
            }
        }

        private boolean validMetricName(String name) {
            return name.matches(VALID_METRIC_NAME_PATTERN);
        }

        private void initMetrics(String name, String state, MetricRegistry metricRegistry) {
            featureUsedCounter = metricRegistry.counter(stringFormat("org.graylog.feature.used.%s", name));
            featureFlagUsedCounter = metricRegistry.counter(stringFormat("org.graylog.featureflag.used.%s", name));
            MetricUtils.getOrRegister(metricRegistry,
                    stringFormat("org.graylog.featureflag.state.%s.%s", name, state), (Gauge<Integer>) () -> 0);
        }

        boolean isOn() {
            incrementCounter(featureFlagUsedCounter);
            return ON.equalsIgnoreCase(value);
        }

        void incrementFeatureIsUsedCounter() {
            incrementCounter(featureUsedCounter);
        }

        private void incrementCounter(Counter counter) {
            if (counter != null) {
                counter.inc();
            }
        }
    }
}
