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
package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_ValueType.Builder.class)
public abstract class ValueType implements Parent {
    static final String VERSION = "1";
    private static final String FIELD_FOOBAR = "foobar";

    @JsonProperty(FIELD_FOOBAR)
    public abstract String foobar();

    public static Builder builder() {
        return new AutoValue_ValueType.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder implements Parent.ParentBuilder<Builder> {
        @JsonProperty(FIELD_FOOBAR)
        public abstract Builder foobar(String foobar);

        abstract ValueType autoBuild();

        public ValueType build() {
            version(VERSION);
            return autoBuild();
        }
    }
}