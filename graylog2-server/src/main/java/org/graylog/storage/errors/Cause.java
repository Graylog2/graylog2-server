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
package org.graylog.storage.errors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = Cause.Builder.class)
public abstract class Cause {

    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String reason();

    @JsonProperty
    @Nullable
    public abstract String indexUuid();

    @JsonProperty
    @Nullable
    public abstract String index();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(final String type);
        @JsonProperty
        public abstract Builder reason(final String reason);
        @JsonProperty
        public abstract Builder indexUuid(final String indexUuid);
        @JsonProperty
        public abstract Builder index(final String index);

        public abstract Cause build();

        @JsonCreator
        public static Cause.Builder builder() {
            return new AutoValue_Cause.Builder();
        }
    }
}
