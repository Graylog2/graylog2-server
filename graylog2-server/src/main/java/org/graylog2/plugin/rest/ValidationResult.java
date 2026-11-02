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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

@JsonAutoDetect
public class ValidationResult {

    private final Multimap<String, String> errors = ArrayListMultimap.create();
    private final Multimap<String, String> context = ArrayListMultimap.create();
    private final Multimap<String, ErrorDetail> errorDetails = ArrayListMultimap.create();

    /**
     * Error code and placeholder values of an error, so that the web interface can show it in the language of the user.
     * The message is the same English text which is in {@code errors}.
     */
    @JsonAutoDetect
    public record ErrorDetail(@JsonProperty("code") String code,
                              @JsonProperty("message") String message,
                              @JsonProperty("params") Map<String, Object> params) {
    }


    public ValidationResult addError(String fieldName, String error) {
        errors.put(fieldName, error);
        return this;
    }

    /**
     * Adds an error with a stable code. The English message is part of {@code errors} as before, the code and the
     * parameters are additionally sent as {@code error_details}.
     */
    public ValidationResult addError(String fieldName, String code, String message, Map<String, Object> params) {
        errors.put(fieldName, message);
        errorDetails.put(fieldName, new ErrorDetail(code, message, Map.copyOf(params)));
        return this;
    }

    public ValidationResult addContext(String fieldName, Iterable<String> values) {
        context.putAll(fieldName, values);
        return this;
    }

    public ValidationResult addAll(Multimap<String, String> extraErrors) {
        errors.putAll(extraErrors);
        return this;
    }

    public ValidationResult addAll(ValidationResult validationResult) {
        errors.putAll(validationResult.errors);
        errorDetails.putAll(validationResult.errorDetails);
        context.putAll(validationResult.context);
        return this;
    }

    @JsonProperty("failed")
    public boolean failed() {
        return !errors.isEmpty();
    }

    @JsonProperty("errors")
    public Map<String, Collection<String>> getErrors() {
        return errors.asMap();
    }

    @JsonProperty("error_details")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Collection<ErrorDetail>> getErrorDetails() {
        return errorDetails.asMap();
    }

    @JsonProperty("error_context")
    public Map<String, Collection<String>> getContext() {
        return context.asMap();
    }
}
