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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize
public abstract class Attribute {

    @JsonProperty("key")
    public abstract String key();

    @JsonProperty("value")
    public abstract Object value();

    public static Builder builder() {
        return new AutoValue_Attribute.Builder();
    }

    public static Attribute of(String key, Object value) {
        return builder().key(key).value(value).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder builder() {
            return Attribute.builder();
        }

        @JsonProperty("key")
        public abstract Builder key(String key);

        @JsonProperty("value")
        public abstract Builder value(Object value);

        public abstract Attribute build();
    }
}
