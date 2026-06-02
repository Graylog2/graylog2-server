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
package org.graylog.events.processor.exclusion;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = Matcher.Builder.class)
public abstract class Matcher {
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_FIELD_NAME = "field_name";
    public static final String FIELD_VALUES = "values";

    @JsonProperty(FIELD_TYPE)
    public abstract MatcherType type();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(FIELD_FIELD_NAME)
    public abstract String fieldName();

    @JsonProperty(FIELD_VALUES)
    public abstract ImmutableList<String> values();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_Matcher.Builder();
        }

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(MatcherType type);

        @JsonProperty(FIELD_FIELD_NAME)
        public abstract Builder fieldName(@Nullable String fieldName);

        @JsonProperty(FIELD_VALUES)
        public abstract Builder values(ImmutableList<String> values);

        abstract Matcher autoBuild();

        public Matcher build() {
            final Matcher m = autoBuild();
            if (m.values().isEmpty()) {
                throw new IllegalArgumentException("Matcher must contain at least one value");
            }
            if (m.type() == MatcherType.FIELD) {
                if (m.fieldName() == null || m.fieldName().isBlank()) {
                    throw new IllegalArgumentException("FIELD matcher requires non-blank fieldName");
                }
            }
            return m;
        }
    }
}
