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
package org.graylog.plugins.views.search.searchtypes.pivot.series;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(Percentage.NAME)
@JsonDeserialize(builder = Percentage.Builder.class)
public abstract class Percentage implements SeriesSpec, HasOptionalField {
    public static final String NAME = "percentage";

    public enum Strategy {
        COUNT,
        SUM
    }

    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @JsonProperty
    public abstract Optional<Strategy> strategy();

    @JsonProperty
    public abstract Optional<String> field();

    @Override
    public String literal() {
        return type() + "(" + field().map(Strings::nullToEmpty).orElse("") + "," + strategy().orElse(Strategy.COUNT) + ")";
    }

    public abstract Builder toBuilder();

    @Override
    public Percentage withId(String id) {
        return toBuilder().id(id).build();
    }

    public static Builder builder() {
        return new AutoValue_Percentage.Builder()
                .type(Percentage.NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends SeriesSpecBuilder<Percentage, Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_Percentage.Builder()
                    .strategy(Strategy.COUNT);
        }

        @Override
        @JsonProperty
        public abstract Builder id(String id);

        abstract Optional<String> id();

        public abstract Builder field(@Nullable String field);

        @JsonProperty("field")
        public Builder nonEmptyField(@Nullable String field) {
            return field(Strings.emptyToNull(field));
        }

        abstract Optional<String> field();

        @JsonProperty
        public abstract Builder strategy(@Nullable Strategy strategy);

        abstract Optional<Strategy> strategy();

        abstract Percentage autoBuild();

        @Override
        public Percentage build() {
            if (id().isEmpty()) {
                id(NAME + "(" + field().map(Strings::nullToEmpty).orElse("") + "," + strategy().orElse(Strategy.COUNT) + ")");
            }

            if (strategy().filter(Strategy.SUM::equals).isPresent() && field().isEmpty()) {
                throw new IllegalArgumentException("When strategy is sum, a field must be specified.");
            }
            return autoBuild();
        }
    }
}
