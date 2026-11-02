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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.util.Map;

@JsonAutoDetect
@AutoValue
@JsonTypeName("ApiError") // Explicitly indicates the class type to avoid AutoValue_ at the beginning
public abstract class ApiError implements GenericError {
    /**
     * Stable identifier of the error, e.g. {@code stream.title_exists}. The web interface translates the error by this
     * code and shows {@link #message()} for codes it does not know. Not part of the response when not set.
     */
    @Nullable
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract String code();

    /**
     * Values of the placeholders of the translated error text, e.g. {@code {"title": "My stream"}}. Not part of the
     * response when not set.
     */
    @Nullable
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Map<String, Object> params();

    public static ApiError create(String message) {
        return new AutoValue_ApiError(message, null, null);
    }

    public static ApiError create(String code, String message, Map<String, Object> params) {
        return new AutoValue_ApiError(message, code, params);
    }

    /**
     * Error for the given exception. Exceptions which implement {@link CodedException} add their code and parameters.
     */
    public static ApiError from(Throwable throwable) {
        if (throwable instanceof CodedException coded) {
            return create(coded.code(), throwable.getMessage(), coded.params());
        }

        return create(throwable.getMessage());
    }
}
