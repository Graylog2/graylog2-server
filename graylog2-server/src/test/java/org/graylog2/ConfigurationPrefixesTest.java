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
package org.graylog2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ConfigurationPrefixesTest {

    private String previousOverride;

    @BeforeEach
    void setUp() {
        previousOverride = System.getProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE);
        assumeTrue(System.getenv(ConfigurationPrefixes.ENV_OVERRIDE) == null);
        System.clearProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE);
    }

    @AfterEach
    void restorePreviousOverride() {
        if (previousOverride == null) {
            System.clearProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE);
        } else {
            System.setProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE, previousOverride);
        }
    }

    @Test
    void usesDefaultsWhenNoOverrideIsSet() {
        assertThat(ConfigurationPrefixes.env()).isEqualTo("GRAYLOG_");
        assertThat(ConfigurationPrefixes.sysProp()).isEqualTo("graylog.");
    }

    @Test
    void overrideReplacesAndCasesThePrefix() {
        System.setProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE, "Custom");
        assertThat(ConfigurationPrefixes.env()).isEqualTo("CUSTOM_");
        assertThat(ConfigurationPrefixes.sysProp()).isEqualTo("custom.");
    }

    @Test
    void trailingSeparatorInOverrideIsNormalized() {
        System.setProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE, "Custom_");
        assertThat(ConfigurationPrefixes.env()).isEqualTo("CUSTOM_");
        assertThat(ConfigurationPrefixes.sysProp()).isEqualTo("custom.");

        System.setProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE, "Custom.");
        assertThat(ConfigurationPrefixes.env()).isEqualTo("CUSTOM_");
        assertThat(ConfigurationPrefixes.sysProp()).isEqualTo("custom.");

        System.setProperty(ConfigurationPrefixes.SYSTEM_PROPERTY_OVERRIDE, "_");
        assertThat(ConfigurationPrefixes.env()).isEqualTo("GRAYLOG_");
    }
}
