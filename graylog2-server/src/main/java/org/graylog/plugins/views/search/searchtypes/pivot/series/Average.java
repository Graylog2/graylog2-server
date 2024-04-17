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
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.searchtypes.pivot.HasField;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;

import java.util.Optional;

@AutoValue
@JsonTypeName(Average.NAME)
@JsonDeserialize(builder = Average.Builder.class)
public abstract class Average implements SeriesSpec, HasField {
    public static final String NAME = "avg";

    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @Override
    @JsonProperty
    public abstract String field();

    @JsonProperty("whole_number")
    public abstract Boolean wholeNumber();

    @Override
    public String literal() {
        return type() + "(" + Strings.nullToEmpty(field()) + ")";
    }

    public abstract Builder toBuilder();

    @Override
    public Average withId(String id) {
        return toBuilder().id(id).build();
    }

    public static Builder builder() {
        return new AutoValue_Average.Builder().type(NAME).wholeNumber(false);
    }

    @AutoValue.Builder
    public abstract static class Builder extends SeriesSpecBuilder<Average, Builder> {
        @JsonCreator
        public static Builder create() {
            return Average.builder().wholeNumber(false);
        }

        @Override
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder field(String field);

        @JsonProperty("whole_number")
        public abstract Builder wholeNumber(Boolean wholeNumber);

        abstract Optional<String> id();
        abstract String field();

        abstract Boolean wholeNumber();
        abstract Average autoBuild();

        @Override
        public Average build() {
            if (id().isEmpty()) {
                id(NAME + "(" + field() + ")");
            }
            if (wholeNumber() != null) {
                wholeNumber(wholeNumber());
            } else {
                wholeNumber(false);
            }
            return autoBuild();
        }
    }
}
