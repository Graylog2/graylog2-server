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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionalStringValidatorTest {
    private OptionalStringValidator validator = new OptionalStringValidator();

    @Test
    public void validateNull() {
        assertThat(validator.validate(null)).isInstanceOf(ValidationResult.ValidationPassed.class);
    }

    @Test
    public void validateEmptyString() {
        assertThat(validator.validate("")).isInstanceOf(ValidationResult.ValidationPassed.class);
    }

    @Test
    public void validateString() {
        assertThat(validator.validate("foobar")).isInstanceOf(ValidationResult.ValidationPassed.class);
    }

    @Test
    public void validateNonString() {
        assertThat(validator.validate(new Object())).isInstanceOf(ValidationResult.ValidationFailed.class);
    }
}