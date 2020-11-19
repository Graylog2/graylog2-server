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
package org.graylog2.rest.models.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Optional;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(builder = AutoValue_RegexValidationResponse.Builder.class)
public abstract class RegexValidationResponse {
    @JsonProperty("regex")
    public abstract String regex();

    @JsonProperty("is_valid")
    public abstract boolean isValid();

    @JsonProperty("validation_message")
    public abstract Optional<String> validationMessage();

    public static Builder builder() {
        return new AutoValue_RegexValidationResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("regex")
        public abstract Builder regex(String regex);

        @JsonProperty("is_valid")
        public abstract Builder isValid(boolean isValid);

        @JsonProperty("validation_message")
        public abstract Builder validationMessage(String validationMessage);

        public abstract RegexValidationResponse build();
    }
}
