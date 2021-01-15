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

import org.bson.types.ObjectId;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;

import javax.annotation.Nullable;

public class ObjectIdValidator implements Validator {
    /**
     * Validates: Object is not {@code null} and of type {@link ObjectId}.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public ValidationResult validate(@Nullable final Object value) {
        if (value instanceof ObjectId) {
            return new ValidationResult.ValidationPassed();
        } else {
            return new ValidationResult.ValidationFailed(String.valueOf(value) + " is not a valid ObjectId!");
        }
    }
}
