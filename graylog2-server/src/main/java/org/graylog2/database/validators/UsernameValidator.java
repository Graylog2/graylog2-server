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

import org.graylog.security.permissions.PermissionInstances;
import org.graylog2.plugin.database.validators.ValidationResult;

/**
 * Validates a username: applies the length checks of {@link LimitedStringValidator} and additionally rejects names
 * that would not address a single user when used as the instance part of a permission string.
 *
 * @see PermissionInstances#isSafe(String)
 */
public class UsernameValidator extends LimitedStringValidator {
    public UsernameValidator(int minLength, int maxLength) {
        super(minLength, maxLength);
    }

    @Override
    public ValidationResult validate(Object value) {
        final ValidationResult result = super.validate(value);
        if (!(result instanceof ValidationResult.ValidationPassed)) {
            return result;
        }

        if (!PermissionInstances.isSafe((String) value)) {
            return new ValidationResult.ValidationFailed("Username \"" + value + "\" is not allowed: it must not "
                    + "contain \"*\", \",\" or \":\" and must not start or end with whitespace.");
        }
        return result;
    }
}
