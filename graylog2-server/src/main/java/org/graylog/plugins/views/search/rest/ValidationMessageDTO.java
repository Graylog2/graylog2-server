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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoValue
public abstract class ValidationMessageDTO {

    @JsonProperty
    public abstract ValidationTypeDTO errorType();

    @JsonProperty
    @Nullable
    public abstract Integer beginLine();

    @JsonProperty
    @Nullable
    public abstract Integer beginColumn();

    @JsonProperty
    @Nullable
    public abstract Integer endLine();

    @JsonProperty
    @Nullable
    public abstract Integer endColumn();

    @JsonProperty
    @Nullable
    public abstract String errorTitle();

    @JsonProperty
    public abstract String errorMessage();

    @Nullable
    @JsonProperty
    public abstract String relatedProperty();

    public static ValidationMessageDTO.Builder builder(ValidationTypeDTO validationType, String errorMessage) {
        return new AutoValue_ValidationMessageDTO.Builder()
                .errorType(validationType)
                .errorMessage(errorMessage);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder errorType(ValidationTypeDTO type);

        public abstract Builder beginLine(int beginLine);

        @Nullable
        public abstract Builder beginColumn(int beginColumn);

        @Nullable
        public abstract Builder endLine(int endLine);

        @Nullable
        public abstract Builder endColumn(int endColumn);

        @Nullable
        public abstract Builder errorTitle(String errorTitle);

        public abstract Builder errorMessage(String errorMessage);

        @Nullable
        public abstract Builder relatedProperty(String relatedProperty);

        public abstract ValidationMessageDTO build();
    }
}
