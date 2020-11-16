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
package org.graylog2.plugin.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.database.validators.ValidationResult;

import java.util.List;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@JsonTypeName("ValidationApiError") // Explicitly indicates the class type to avoid AutoValue_ at the beginning
public abstract class ValidationApiError implements GenericError {
    @JsonProperty
    public abstract Map<String, List<ValidationResult>> validationErrors();

    public static ValidationApiError create(String message, Map<String, List<ValidationResult>> validationErrors) {
        return new AutoValue_ValidationApiError(message, ImmutableMap.copyOf(validationErrors));
    }
}
