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
package org.graylog.events.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.events.fields.validators.FieldTypeValidator;
import org.graylog.events.fields.validators.NoopFieldTypeValidator;

import java.util.List;
import java.util.Optional;

public enum FieldValueType implements FieldTypeValidator {
    @JsonProperty("string")
    STRING(NoopFieldTypeValidator.INSTANCE),
    @JsonProperty("error")
    ERROR(NoopFieldTypeValidator.INSTANCE, true);

    private final FieldTypeValidator validator;
    private final boolean isError;

    FieldValueType(FieldTypeValidator validator) {
        this(validator, false);
    }

    FieldValueType(FieldTypeValidator validator, boolean isError) {
        this.validator = validator;
        this.isError = isError;
    }

    public boolean isError() {
        return isError;
    }

    @Override
    public Optional<List<String>> validate(String value) {
        return validator.validate(value);
    }
}
