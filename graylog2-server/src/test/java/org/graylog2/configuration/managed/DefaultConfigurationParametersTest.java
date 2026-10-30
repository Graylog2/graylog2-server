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
package org.graylog2.configuration.managed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultConfigurationParametersTest {
    private static final List<ConfigurationParameter> PARAMETERS = DefaultConfigurationParameters.all();

    private static ConfigurationParameter parameter(String name) {
        return PARAMETERS.stream()
                .filter(parameter -> parameter.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Parameter <" + name + "> isn't registered"));
    }

    @Test
    void everyPatternCompilesAndStaysPortable() {
        PARAMETERS.forEach(parameter -> {
            final String pattern = parameter.constraints().pattern();
            if (pattern == null) {
                return;
            }
            // The user interface applies the same patterns, where Java-only constructs would throw
            assertThat(pattern)
                    .describedAs("pattern of <%s> must not use inline flags", parameter.name())
                    .doesNotContain("(?i)", "(?s)", "(?m)", "(?<");
            assertThat(Pattern.compile(pattern)).isNotNull();
        });
    }

    @Test
    void cheapGuardsAgainstTypos() {
        PARAMETERS.forEach(parameter -> {
            assertThat(parameter.label()).describedAs("label of <%s>", parameter.name()).isNotBlank();
            assertThat(parameter.description()).describedAs("description of <%s>", parameter.name()).isNotBlank();
            assertThat(parameter.category()).describedAs("category of <%s>", parameter.name()).isNotBlank();
        });
    }

    @Test
    void runtimeParametersAreNotReportedByNodes() {
        // A runtime parameter has no configuration bean, so ConfigurationInfoService never knows a value for it
        assertThat(PARAMETERS)
                .filteredOn(parameter -> parameter.backing() == ParameterBacking.RUNTIME)
                .isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "opensearch_heap, 2g, true",
            "opensearch_heap, 512M, true",
            "opensearch_heap, 0g, false",
            "opensearch_heap, 2gb, false",
            "node_roles, data, true",
            "node_roles, 'cluster_manager,data', true",
            "node_roles, coordinator, false",
            "cluster.routing.allocation.disk.watermark.low, 85%, true",
            "cluster.routing.allocation.disk.watermark.low, 100%, true",
            "cluster.routing.allocation.disk.watermark.low, 0%, true",
            "cluster.routing.allocation.disk.watermark.low, 101%, false",
            "cluster.routing.allocation.disk.watermark.low, 150%, false",
            "cluster.routing.allocation.disk.watermark.low, 0.85, true",
            "cluster.routing.allocation.disk.watermark.low, 1.5, false",
            "cluster.routing.allocation.disk.watermark.low, 100GB, true",
            "cluster.routing.allocation.disk.watermark.low, plenty, false",
            "cluster.max_shards_per_node, 1000, true",
            "cluster.max_shards_per_node, 0, false",
            "indices.recovery.max_bytes_per_sec, 40mb, true",
            "indices.recovery.max_bytes_per_sec, 40%, false",
            "indices.queries.cache.size, 10%, true",
            "indices.queries.cache.size, 512mb, true"
    })
    void acceptsAndRejectsRepresentativeValues(String name, String value, boolean valid) {
        assertThat(parameter(name).validate(value).isEmpty())
                .describedAs("<%s> = <%s>", name, value)
                .isEqualTo(valid);
    }
}
