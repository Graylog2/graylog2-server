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

import javax.annotation.Nullable;
import java.util.Optional;

@JsonAutoDetect
@AutoValue
@JsonTypeName("RequestError") // Explicitly indicates the class type to avoid AutoValue_ at the beginning
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class RequestError implements GenericError {
    @JsonProperty
    public abstract int line();

    @JsonProperty
    public abstract int column();

    @JsonProperty
    public abstract String path();

    @JsonProperty
    public abstract Optional<String> referencePath();

    public static RequestError create(String message, int line, int column, String path, @Nullable String reference) {
        return new AutoValue_RequestError(message, line, column, path, Optional.ofNullable(reference));
    }
}
