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

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationParameterTest {

    private static ConfigurationParameter parameter(ParameterType type, ParameterConstraints constraints) {
        return ConfigurationParameter.builder(NodeType.SERVER, "a_parameter", "A parameter")
                .type(type)
                .constraints(constraints)
                .build();
    }

    @Test
    void rejectsMissingValues() {
        final var parameter = parameter(ParameterType.STRING, ParameterConstraints.none());

        assertThat(parameter.validate(null)).hasValueSatisfying(m -> assertThat(m).contains("a_parameter"));
        assertThat(parameter.validate("   ")).isPresent();
        assertThat(parameter.validate("anything")).isEmpty();
    }

    @Test
    void checksIntegerRange() {
        final var parameter = parameter(ParameterType.INTEGER, ParameterConstraints.range(1L, 10L));

        assertThat(parameter.validate("1")).isEmpty();
        assertThat(parameter.validate("10")).isEmpty();
        assertThat(parameter.validate("0")).hasValueSatisfying(m -> assertThat(m).contains("smaller"));
        assertThat(parameter.validate("11")).hasValueSatisfying(m -> assertThat(m).contains("larger"));
        assertThat(parameter.validate("eight")).hasValueSatisfying(m -> assertThat(m).contains("not a number"));
    }

    @Test
    void checksBooleans() {
        final var parameter = parameter(ParameterType.BOOLEAN, ParameterConstraints.none());

        assertThat(parameter.validate("true")).isEmpty();
        assertThat(parameter.validate("FALSE")).isEmpty();
        assertThat(parameter.validate("yes")).isPresent();
    }

    @Test
    void checksPattern() {
        final var parameter = parameter(ParameterType.SIZE, ParameterConstraints.pattern("[1-9][0-9]*[gGmMkK]"));

        assertThat(parameter.validate("2g")).isEmpty();
        assertThat(parameter.validate(" 512m ")).isEmpty();
        assertThat(parameter.validate("0g")).isPresent();
        assertThat(parameter.validate("2gb")).isPresent();
    }

    @Test
    void checksEveryElementOfAList() {
        final var parameter = parameter(ParameterType.LIST, ParameterConstraints.allowedValues("data", "search"));

        assertThat(parameter.validate("data")).isEmpty();
        assertThat(parameter.validate("data, search")).isEmpty();
        assertThat(parameter.validate("data,ingest")).hasValueSatisfying(m -> assertThat(m).contains("ingest"));
        assertThat(parameter.validate("data,")).hasValueSatisfying(m -> assertThat(m).contains("empty"));
    }

    @Test
    void usesDefaultsForOptionalDescriptorFields() {
        final var parameter = ConfigurationParameter.builder(NodeType.DATANODE, "a_parameter", "A parameter").build();

        assertThat(parameter.description()).isEmpty();
        assertThat(parameter.category()).isEmpty();
        assertThat(parameter.constraints()).isEqualTo(ParameterConstraints.none());
        assertThat(parameter.type()).isEqualTo(ParameterType.STRING);
        assertThat(parameter.backing()).isEqualTo(ParameterBacking.CONFIG_FILE);
        assertThat(parameter.scope()).isEqualTo(ParameterScope.NODE);
        assertThat(parameter.requiresRestart()).isTrue();
    }
}
