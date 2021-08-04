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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ImmutableFeatureFlagsTest {

    private static final String PREFIX_SYSTEM_PROPERTY = "graylog.feature.";
    private static final String PREFIX_ENVIRONMENT_VARIABLE = "GRAYLOG_FEATURE_";

    private static final String FEATURE_1 = "f1";
    private static final String FEATURE_2 = "f2";
    private static final String FEATURE_3 = "f3";
    private static final String FEATURE_4 = "f4";
    private static final String DEFAULT_PROPERTY_VALUE = "default_prop";
    private static final String CUSTOM_PROPERTY_VALUE = "custom_prop";
    private static final String SYSTEM_PROPERTY_VALUE = "system_prop";
    private static final String ENVIRONMENT_VARIABLE_VALUE = "env_var";

    private static final Map<String, String> EMPTY = ImmutableMap.of();
    public static final String FILE = "file";
    private final FeatureFlagsFactory factory = new FeatureFlagsFactory();

    @Mock
    FeatureFlagsResources featureFlagsResources;

    MetricRegistry metricRegistry = new MetricRegistry();

    @Test
    void testOverrideOrder() throws IOException {
        Map<String, String> defaultProps = ImmutableMap.of(
                FEATURE_1, DEFAULT_PROPERTY_VALUE,
                FEATURE_2, DEFAULT_PROPERTY_VALUE,
                FEATURE_3, DEFAULT_PROPERTY_VALUE,
                FEATURE_4, DEFAULT_PROPERTY_VALUE);
        Map<String, String> customProps = ImmutableMap.of(
                FEATURE_2, CUSTOM_PROPERTY_VALUE,
                FEATURE_3, CUSTOM_PROPERTY_VALUE,
                FEATURE_4, CUSTOM_PROPERTY_VALUE);
        Map<String, String> systemProps = ImmutableMap.of(
                PREFIX_SYSTEM_PROPERTY + FEATURE_3, SYSTEM_PROPERTY_VALUE,
                PREFIX_SYSTEM_PROPERTY + FEATURE_4, SYSTEM_PROPERTY_VALUE);
        Map<String, String> envVars = ImmutableMap.of(
                PREFIX_ENVIRONMENT_VARIABLE + FEATURE_4, ENVIRONMENT_VARIABLE_VALUE);

        FeatureFlags flags = create(defaultProps, customProps, systemProps, envVars);

        assertThat(flags.getAll()).isEqualTo(ImmutableMap.of(
                FEATURE_1, DEFAULT_PROPERTY_VALUE,
                FEATURE_2, CUSTOM_PROPERTY_VALUE,
                FEATURE_3, SYSTEM_PROPERTY_VALUE,
                FEATURE_4, ENVIRONMENT_VARIABLE_VALUE
        ));
    }

    @Test
    void testSystemPropertyPrefix() throws IOException {
        FeatureFlags flags = create(EMPTY, EMPTY, ImmutableMap.of(
                "wrong prefix", SYSTEM_PROPERTY_VALUE,
                PREFIX_SYSTEM_PROPERTY + FEATURE_1, SYSTEM_PROPERTY_VALUE), EMPTY);

        assertThat(flags.getAll()).isEqualTo(ImmutableMap.of(FEATURE_1, SYSTEM_PROPERTY_VALUE));
    }

    @Test
    void testEnvironmentVariablePrefix() throws IOException {
        FeatureFlags flags = create(EMPTY, EMPTY, EMPTY, ImmutableMap.of(
                "wrong prefix", ENVIRONMENT_VARIABLE_VALUE,
                PREFIX_ENVIRONMENT_VARIABLE + FEATURE_1, ENVIRONMENT_VARIABLE_VALUE));

        assertThat(flags.getAll()).isEqualTo(ImmutableMap.of(FEATURE_1, ENVIRONMENT_VARIABLE_VALUE));
    }

    @ParameterizedTest
    @CsvSource({"f1,F1,on,true", "F1,f1,ON,true", "F1,F1,OFF,false"})
    void testGetBoolFeatureFlag(String init, String feature, String value, boolean expected) throws IOException {
        FeatureFlags flags = create(init, value);

        boolean on = flags.isOn(feature, false);

        assertThat(on).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testGetBoolFeatureFlagDefaultValue(boolean defaultValue) throws IOException {
        FeatureFlags flags = empty();

        boolean on = flags.isOn("notExist", defaultValue);

        assertThat(on).isEqualTo(defaultValue);
    }

    @Test
    void testFeatureFlagResourcesCouldBeRead() throws Exception {
        String file = Objects.requireNonNull(this.getClass()
                .getResource("/org/graylog2/featureflag/custom-feature-flag.config")).toURI().getPath();

        FeatureFlags flags = factory.createImmutableFeatureFlags(file, metricRegistry);

        assertThat(flags.getAll().keySet()).contains("feature1");
    }

    @Test
    void testNoDuplicateFeatureFlagNamesAreAllowedWithinDefaultProperties() {
        assertThatIllegalStateException().isThrownBy(() -> mockAndCreate(() ->
                given(featureFlagsResources.defaultProperties(any())).willReturn(duplicate())));
    }

    @Test
    void testNoDuplicateFeatureFlagNamesAreAllowedWithinCustomProperties() {
        assertThatIllegalStateException().isThrownBy(() -> mockAndCreate(() ->
                given(featureFlagsResources.customProperties(any())).willReturn(duplicate())));
    }

    @Test
    void testNoDuplicateFeatureFlagNamesAreAllowedWithinSystemProperties() {
        assertThatIllegalStateException().isThrownBy(() -> mockAndCreate(() ->
                given(featureFlagsResources.systemProperties()).willReturn(duplicate(PREFIX_SYSTEM_PROPERTY))));
    }

    @Test
    void testNoDuplicateFeatureFlagNamesAreAllowedWithinEnvironmentVariables() {
        assertThatIllegalStateException().isThrownBy(() -> mockAndCreate(() ->
                given(featureFlagsResources.environmentVariables()).willReturn(duplicate(PREFIX_ENVIRONMENT_VARIABLE))));
    }

    private ImmutableMap<String, String> duplicate() {
        return duplicate("");
    }

    private ImmutableMap<String, String> duplicate(String prefix) {
        String key = prefix + "feature";
        return ImmutableMap.of(
                key.toUpperCase(Locale.ROOT), "on",
                key.toLowerCase(Locale.ROOT), "on");
    }

    private FeatureFlags create(String feature, String value) throws IOException {
        return create(ImmutableMap.of(feature, value), EMPTY, EMPTY, EMPTY);
    }

    private FeatureFlags empty() throws IOException {
        return create(EMPTY, EMPTY, EMPTY, EMPTY);
    }

    private FeatureFlags create(Map<String, String> defaultProperties,
                                Map<String, String> customProperties,
                                Map<String, String> systemProperties,
                                Map<String, String> environmentVariables) throws IOException {

        return mockAndCreate(() -> {
            given(featureFlagsResources.defaultProperties(any())).willReturn(defaultProperties);
            given(featureFlagsResources.customProperties(any())).willReturn(customProperties);
            given(featureFlagsResources.systemProperties()).willReturn(systemProperties);
            given(featureFlagsResources.environmentVariables()).willReturn(environmentVariables);
        });
    }

    private FeatureFlags mockAndCreate(Action action) throws IOException {
        action.execute();
        return factory.createImmutableFeatureFlags(featureFlagsResources, FILE, FILE, metricRegistry);
    }

    interface Action {
        void execute() throws IOException;
    }
}
