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
import org.graylog2.plugin.database.validators.Validator;

import java.util.List;

public class ListValidator implements Validator {
    private boolean allowMissing;

    public ListValidator() {
        this(false);
    }

    public ListValidator(boolean allowNull) {
        this.allowMissing = allowNull;
    }

    @Override
    public ValidationResult validate(Object value) {
        if ((allowMissing && value == null) || value instanceof List) {
            return new ValidationResult.ValidationPassed();
        } else {
            return new ValidationResult.ValidationFailed("Value is not a list!");
        }
    }
}
