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
package org.graylog.plugins.views.search.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FieldTypeValidationTest {

    private FieldTypeValidation fieldTypeValidation;

    @BeforeEach
    void setUp() {
        fieldTypeValidation = new FieldTypeValidationImpl();
    }

    @Test
    void validateFieldValueType() {
        assertThat(fieldTypeValidation.validateFieldValueType(term("123"), "long")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("ABC"), "long")).isPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2020-07-29T12:00:00.000-05:00"), "date")).isNotPresent();
    }

    private ParsedTerm term(String value) {
        return ParsedTerm.builder()
                .field("foo")
                .value(value)
                .build();
    }
}
