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
import org.graylog.plugins.views.search.engine.validation.ValidationMessage;

import javax.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoValue
public abstract class ValidationExplanationDTO {
    @JsonProperty
    public abstract String index();

    @JsonProperty
    public abstract boolean valid();

    @Nullable
    @JsonProperty
    public abstract ValidationMessage message();

    public static Builder builder() {
        return new AutoValue_ValidationExplanationDTO.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder index(final String index);
        public abstract Builder valid(final boolean valid);
        public abstract Builder message(final ValidationMessage message);
        public abstract ValidationExplanationDTO build();
    }
}
