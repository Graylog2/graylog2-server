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
package org.graylog.plugins.views.search.engine.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoValue
public abstract class ValidationMessage {

    @JsonProperty
    @Nullable
    public abstract Integer line();

    @JsonProperty
    @Nullable
    public abstract Integer column();

    @JsonProperty
    @Nullable
    public abstract String errorType();

    @JsonProperty
    public abstract String errorMessage();

    public static Builder builder() {
        return new AutoValue_ValidationMessage.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder line(int line);

        public abstract Builder column(int column);

        public abstract Builder errorType(@Nullable String errorType);

        public abstract Builder errorMessage(String errorMessage);

        public abstract Optional<String> errorMessage();

        public abstract ValidationMessage build();
    }

}
