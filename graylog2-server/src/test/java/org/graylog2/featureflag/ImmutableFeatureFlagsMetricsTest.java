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
import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class ImmutableFeatureFlagsMetricsTest {

    public static final String FEATURE = "feature1";
    public static final String STATE = "on";

    @Mock
    FeatureFlagsResources resources;
    MetricRegistry metricRegistry;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
    }

    @Test
    void testMetricsInitialized() throws IOException {
        createFeatureFlags();

        assertThat(getFeatureFlagUsedCount()).isEqualTo(0);
        assertThat(getFeatureUsedCount()).isEqualTo(0);
        assertThat(featureStateGaugeExist()).isTrue();
    }

    @Test
    void testIncrementFeatureFlagIsUsedCounter() throws IOException {
        FeatureFlags underTest = createFeatureFlags();

        underTest.isOn(FEATURE, false);

        assertThat(getFeatureFlagUsedCount()).isEqualTo(1);
    }

    @Test
    void testIncrementFeatureIsUsedCounter() throws IOException {
        FeatureFlags underTest = createFeatureFlags();

        underTest.incrementFeatureIsUsedCounter(FEATURE);

        assertThat(getFeatureUsedCount()).isEqualTo(1);
    }

    @Test
    void testIncrementFeatureIsUsedCounterForNotExistingFeature() throws IOException {
        FeatureFlags underTest = createFeatureFlags();

        underTest.incrementFeatureIsUsedCounter("not exist");

        assertThat(metricRegistry.getCounters().size()).isEqualTo(2);
        assertThat(getFeatureUsedCount()).isEqualTo(0);
    }

    @Test
    void testInvalidFeatureFlagNameDoNotHaveMetrics() throws IOException {
        createFeatureFlags(Maps.of("ignore.metric", "on"));

        assertThat(metricRegistry.getCounters()).isEmpty();
        assertThat(metricRegistry.getGauges()).isEmpty();
    }

    private FeatureFlags createFeatureFlags() throws IOException {
        return createFeatureFlags(Maps.of(FEATURE, STATE));
    }

    private FeatureFlags createFeatureFlags(Map<String, String> flags) throws IOException {
        given(resources.defaultProperties(any())).willReturn(flags);
        return new FeatureFlagsFactory().createImmutableFeatureFlags(resources, "file", "file", metricRegistry);
    }

    private long getFeatureFlagUsedCount() {
        return metricRegistry.getCounters().get(
                String.format(Locale.ROOT, "org.graylog.featureflag.used.%s", FEATURE)).getCount();
    }

    private long getFeatureUsedCount() {
        return metricRegistry.getCounters().get(
                String.format(Locale.ROOT, "org.graylog.feature.used.%s", FEATURE)).getCount();
    }

    private boolean featureStateGaugeExist() {
        return metricRegistry.getGauges().get(
                String.format(Locale.ROOT, "org.graylog.featureflag.state.%s.%s", FEATURE, STATE)) != null;
    }
}
