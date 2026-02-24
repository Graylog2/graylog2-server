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
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = NumberRange.Builder.class)
public abstract class NumberRange {

    @JsonProperty
    public abstract Optional<Double> from();

    @JsonProperty
    public abstract Optional<Double> to();

    public static NumberRange create(@Nullable Double from, @Nullable Double to) {
        final Builder builder = builder();
        if (from != null) {
            builder.from(from);
        }
        if (to != null) {
            builder.to(to);
        }
        return builder.build();
    }

    public static NumberRange.Builder builder() {
        return new AutoValue_NumberRange.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return NumberRange.builder();
        }

        @JsonProperty
        public abstract Builder from(Double from);

        @JsonProperty
        public abstract Builder to(Double to);

        public abstract NumberRange build();
    }
}
