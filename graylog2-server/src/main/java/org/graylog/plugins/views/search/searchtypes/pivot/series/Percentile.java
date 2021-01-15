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
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import java.util.Optional;

@AutoValue
@JsonTypeName(Percentile.NAME)
@JsonDeserialize(builder = Percentile.Builder.class)
public abstract class Percentile implements SeriesSpec {
    public static final String NAME = "percentile";

    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract Double percentile();

    @Override
    public String literal() {
        return type() + "(" + field() + "," + percentile() + ")";
    }

    public static Builder builder() {
        return new AutoValue_Percentile.Builder().type(NAME);
    }

    @AutoValue.Builder
    public abstract static class Builder extends TypedBuilder<Percentile, Builder> {
        @JsonCreator
        public  static Builder create() { return Percentile.builder(); }

        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder field(String field);

        @JsonProperty
        public abstract Builder percentile(Double percentile);

        abstract Optional<String> id();
        abstract String field();
        abstract Double percentile();
        abstract Percentile autoBuild();

        public Percentile build() {
            if (!id().isPresent()) {
                id(NAME + "(" + field() + "," + percentile().toString() + ")");
            }
            return autoBuild();
        }
    }
}
