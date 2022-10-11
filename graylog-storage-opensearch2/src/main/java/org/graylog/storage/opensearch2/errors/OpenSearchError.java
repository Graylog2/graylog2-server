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

import java.util.List;

@AutoValue
@JsonDeserialize(builder = OpenSearchError.Builder.class)
public abstract class OpenSearchError {
    public abstract List<Cause> rootCause();
    public abstract String type();
    public abstract String reason();
    public abstract String phase();
    public abstract boolean grouped();
    public abstract List<FailedShard> failedShards();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder rootCause(final List<Cause> rootCause);
        @JsonProperty
        public abstract Builder type(final String type);
        @JsonProperty
        public abstract Builder reason(final String reason);
        @JsonProperty
        public abstract Builder phase(final String phrase);
        @JsonProperty
        public abstract Builder grouped(final boolean gruped);
        @JsonProperty
        public abstract Builder failedShards(final List<FailedShard> failedShards);
        public abstract OpenSearchError build();

        @JsonCreator
        public static OpenSearchError.Builder builder() {
            return new AutoValue_OpenSearchError.Builder();
        }
    }
}
