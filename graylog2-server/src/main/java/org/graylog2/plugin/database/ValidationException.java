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
package org.graylog2.plugin.database;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import org.graylog2.plugin.database.validators.ValidationResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ValidationException extends Exception {
    private final Map<String, List<ValidationResult>> errors;

    public ValidationException(Map<String, List<ValidationResult>> errors) {
        this.errors = ImmutableMap.copyOf(errors);
    }

    public ValidationException(final String message) {
        this("_", message);
    }

    public ValidationException(final String field, final String message) {
        super(message);
        this.errors = ImmutableMap.of(field, Collections.<ValidationResult>singletonList(new ValidationResult.ValidationFailed(message)));
    }

    public Map<String, List<ValidationResult>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("message", getLocalizedMessage())
            .add("errors", errors)
            .toString();
    }
}
