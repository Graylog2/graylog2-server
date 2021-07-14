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

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.featureflag.FeatureFlagsResources.FeatureFlagResource;

class StaticFeatureFlagsTest {

    private static final String PREFIX_SYSTEM_PROPERTY = "graylog.ff.";
    private static final String PREFIX_ENVIRONMENT_VARIABLE = "GRAYLOG_FF_";

    private static final String FEATURE_1 = "f1";
    private static final String FEATURE_2 = "f2";
    private static final String FEATURE_3 = "f3";
    private static final String FEATURE_4 = "f4";
    private static final String DEFAULT_PROPERTY_VALUE = "default_prop";
    private static final String CUSTOM_PROPERTY_VALUE = "custom_prop";
    private static final String SYSTEM_PROPERTY_VALUE = "system_prop";
    private static final String ENVIRONMENT_VARIABLE_VALUE = "env_var";

    private static final FeatureFlagResource EMPTY = Maps::newHashMap;

    private final FeatureFlagsFactory factory = new FeatureFlagsFactory();

    @Test
    void testOverrideOrder() {
        FeatureFlagResource defaultProperties = () -> ImmutableMap.of(
                FEATURE_1, DEFAULT_PROPERTY_VALUE,
                FEATURE_2, DEFAULT_PROPERTY_VALUE,
                FEATURE_3, DEFAULT_PROPERTY_VALUE,
                FEATURE_4, DEFAULT_PROPERTY_VALUE);
        FeatureFlagResource customProperties = () -> ImmutableMap.of(
                FEATURE_2, CUSTOM_PROPERTY_VALUE,
                FEATURE_3, CUSTOM_PROPERTY_VALUE,
                FEATURE_4, CUSTOM_PROPERTY_VALUE);
        FeatureFlagResource systemProperties = () -> ImmutableMap.of(
                PREFIX_SYSTEM_PROPERTY + FEATURE_3, SYSTEM_PROPERTY_VALUE,
                PREFIX_SYSTEM_PROPERTY + FEATURE_4, SYSTEM_PROPERTY_VALUE);
        FeatureFlagResource environmentVariables = () -> ImmutableMap.of(
                PREFIX_ENVIRONMENT_VARIABLE + FEATURE_4, ENVIRONMENT_VARIABLE_VALUE);

        FeatureFlags flags = create(defaultProperties, customProperties, systemProperties, environmentVariables);

        assertThat(flags.getAll()).isEqualTo(ImmutableMap.of(
                FEATURE_1, DEFAULT_PROPERTY_VALUE,
                FEATURE_2, CUSTOM_PROPERTY_VALUE,
                FEATURE_3, SYSTEM_PROPERTY_VALUE,
                FEATURE_4, ENVIRONMENT_VARIABLE_VALUE
        ));
    }

    @Test
    void testSystemPropertyPrefix() {
        FeatureFlags flags = create(EMPTY, EMPTY, () -> ImmutableMap.of(
                "wrong prefix", SYSTEM_PROPERTY_VALUE,
                PREFIX_SYSTEM_PROPERTY + FEATURE_1, SYSTEM_PROPERTY_VALUE), EMPTY);

        assertThat(flags.getAll()).isEqualTo(ImmutableMap.of(FEATURE_1, SYSTEM_PROPERTY_VALUE));
    }

    @Test
    void testEnvironmentVariablePrefix() {
        FeatureFlags flags = create(EMPTY, EMPTY, EMPTY, () -> ImmutableMap.of(
                "wrong prefix", ENVIRONMENT_VARIABLE_VALUE,
                PREFIX_ENVIRONMENT_VARIABLE + FEATURE_1, ENVIRONMENT_VARIABLE_VALUE));

        assertThat(flags.getAll()).isEqualTo(ImmutableMap.of(FEATURE_1, ENVIRONMENT_VARIABLE_VALUE));
    }

    @ParameterizedTest
    @CsvSource({"f1,F1,on,true", "F1,f1,ON,true", "F1,F1,OFF,false"})
    void testGetBoolFeatureFlag(String init, String feature, String value, boolean expected) {
        FeatureFlags flags = create(init, value);

        boolean on = flags.isOn(feature, false);

        assertThat(on).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testGetBoolFeatureFlagDefaultValue(boolean defaultValue) {
        FeatureFlags flags = empty();

        boolean on = flags.isOn("notExist", defaultValue);

        assertThat(on).isEqualTo(defaultValue);
    }


    private FeatureFlags create(String feature, String value) {
        return create(() -> ImmutableMap.of(feature, value), EMPTY, EMPTY, EMPTY);
    }

    private FeatureFlags empty() {
        return create(EMPTY, EMPTY, EMPTY, EMPTY);
    }


    private FeatureFlags create(FeatureFlagResource defaultProperties,
                                FeatureFlagResource customProperties,
                                FeatureFlagResource systemProperties,
                                FeatureFlagResource environmentVariables) {
        return factory.createStaticFeatureFlags(FeatureFlagsResources.create(
                defaultProperties, customProperties, systemProperties, environmentVariables));
    }
}
