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
package org.graylog2.database.validators;

import org.graylog2.plugin.database.validators.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LimitedOptionalStringValidatorTest {
    @Test
    public void testNegativeMaxLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LimitedOptionalStringValidator(-1);
        });
    }

    @Test
    public void testZeroMaxLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LimitedOptionalStringValidator(0);
        });
    }

    @Test
    public void testValidateNullValue() {
        assertThat(new LimitedOptionalStringValidator(1).validate(null))
                .isInstanceOf(ValidationResult.ValidationPassed.class);
    }

    @Test
    public void testValidateEmptyValue() {
        assertThat(new LimitedOptionalStringValidator(1).validate(""))
                .isInstanceOf(ValidationResult.ValidationPassed.class);
    }

    @Test
    public void testValidateLongString() {
        assertThat(new LimitedOptionalStringValidator(1).validate("12"))
                .isInstanceOf(ValidationResult.ValidationFailed.class);
    }

    @Test
    public void testValidateNoString() {
        assertThat(new LimitedOptionalStringValidator(1).validate(123))
                .isInstanceOf(ValidationResult.ValidationFailed.class);
    }

    @Test
    public void testValidateMaxLengthString() {
        assertThat(new LimitedOptionalStringValidator(5).validate("12345"))
                .isInstanceOf(ValidationResult.ValidationPassed.class);
    }
}