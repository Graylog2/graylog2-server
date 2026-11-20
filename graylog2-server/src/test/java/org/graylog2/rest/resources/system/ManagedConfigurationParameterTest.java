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
package org.graylog2.rest.resources.system;

import org.graylog2.configuration.managed.ConfigurationParameter;
import org.graylog2.configuration.managed.ConfigurationSource;
import org.graylog2.configuration.managed.ManagedConfigurationEntry;
import org.graylog2.configuration.managed.NodeType;
import org.graylog2.configuration.managed.ParameterBacking;
import org.graylog2.configuration.managed.ParameterScope;
import org.graylog2.rest.resources.system.ManagedConfigurationResource.ManagedConfigurationParameter;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedConfigurationParameterTest {
    private static final String NAME = "cluster.routing.allocation.disk.watermark.low";
    private static final ConfigurationParameter PARAMETER = ConfigurationParameter.builder(NodeType.DATANODE, NAME, "Low")
            .backing(ParameterBacking.RUNTIME).requiresRestart(false).scope(ParameterScope.CLUSTER)
            .defaultValue("85%")
            .build();

    @Test
    void showsTheDefaultWhileNothingIsKnown() {
        final var parameter = ManagedConfigurationParameter.of(PARAMETER, null, false);

        assertThat(parameter.value()).isEqualTo("85%");
        assertThat(parameter.source()).isEqualTo(ConfigurationSource.DEFAULT);
        assertThat(parameter.nodeValue()).isEqualTo("85%");
        assertThat(parameter.nodeSource()).isEqualTo(ConfigurationSource.DEFAULT);
        assertThat(parameter.setValue()).isNull();
    }

    @Test
    void storedValueWinsOverTheDefault() {
        final var entry = new ManagedConfigurationEntry(null, null, NAME, null, null, null, "80%", Instant.now(), "admin",
                Instant.now());

        final var parameter = ManagedConfigurationParameter.of(PARAMETER, entry, false);

        assertThat(parameter.value()).isEqualTo("80%");
        assertThat(parameter.source()).isEqualTo(ConfigurationSource.DATABASE);
        // What applies once the stored value is removed
        assertThat(parameter.nodeValue()).isEqualTo("85%");
        assertThat(parameter.nodeSource()).isEqualTo(ConfigurationSource.DEFAULT);
    }

    @Test
    void reportedValueWinsOverTheDefault() {
        final var entry = new ManagedConfigurationEntry(null, null, NAME, "90%", ConfigurationSource.CONFIG_FILE,
                Instant.now(), null, null, null, null);

        final var parameter = ManagedConfigurationParameter.of(PARAMETER, entry, false);

        assertThat(parameter.value()).isEqualTo("90%");
        assertThat(parameter.source()).isEqualTo(ConfigurationSource.CONFIG_FILE);
    }

    @Test
    void staysEmptyWithoutADefault() {
        final var withoutDefault = ConfigurationParameter.builder(NodeType.DATANODE, "node_roles", "Roles").build();

        final var parameter = ManagedConfigurationParameter.of(withoutDefault, null, false);

        assertThat(parameter.value()).isNull();
        assertThat(parameter.source()).isNull();
    }
}
