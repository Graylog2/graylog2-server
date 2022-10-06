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
package org.graylog.storage.opensearch2.errors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = ResponseError.Builder.class)
public abstract class ResponseError {
    public abstract OpenSearchError error();
    public abstract int status();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder error(final OpenSearchError error);
        @JsonProperty
        public abstract Builder status(final int status);
        @JsonProperty
        public abstract ResponseError build();

        @JsonCreator
        public static ResponseError.Builder builder() {
            return new AutoValue_ResponseError.Builder();
        }
    }
}
