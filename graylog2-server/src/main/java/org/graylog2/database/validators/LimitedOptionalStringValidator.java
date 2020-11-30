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

import static com.google.common.base.Preconditions.checkArgument;

public class LimitedOptionalStringValidator extends OptionalStringValidator {
    private final int maxLength;

    public LimitedOptionalStringValidator(int maxLength) {
        checkArgument(maxLength > 0, "maxLength must be greater than 0");
        this.maxLength = maxLength;
    }

    @Override
    public ValidationResult validate(Object value) {
        ValidationResult result = super.validate(value);

        if (result instanceof ValidationResult.ValidationPassed) {
            final String sValue = (String) value;
            if (sValue != null && sValue.length() > maxLength) {
                result = new ValidationResult.ValidationFailed("Value is longer than " + maxLength + " characters!");
            }
        }

        return result;
    }
}
