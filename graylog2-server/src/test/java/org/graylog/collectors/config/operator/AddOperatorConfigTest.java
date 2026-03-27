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
package org.graylog.collectors.config.operator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddOperatorConfigTest {

    @Test
    void createsStringAttributeOperator() {
        final var operator = AddOperatorConfig.forAttribute("glc.receiver.type", "filelog");

        assertThat(operator.type()).isEqualTo("add");
        assertThat(operator.field()).isEqualTo("attributes[\"glc.receiver.type\"]");
        assertThat(operator.value().isTextual()).isTrue();
        assertThat(operator.value().textValue()).isEqualTo("filelog");
    }

    @Test
    void createsNumericAttributeOperator() {
        final var operator = AddOperatorConfig.forAttribute("retries", 3);

        assertThat(operator.type()).isEqualTo("add");
        assertThat(operator.field()).isEqualTo("attributes[\"retries\"]");
        assertThat(operator.value().isNumber()).isTrue();
        assertThat(operator.value().numberValue()).isEqualTo(3);
    }

    @Test
    void createsBooleanAttributeOperator() {
        final var operator = AddOperatorConfig.forAttribute("enabled", true);

        assertThat(operator.type()).isEqualTo("add");
        assertThat(operator.field()).isEqualTo("attributes[\"enabled\"]");
        assertThat(operator.value().isBoolean()).isTrue();
        assertThat(operator.value().booleanValue()).isTrue();
    }
}
