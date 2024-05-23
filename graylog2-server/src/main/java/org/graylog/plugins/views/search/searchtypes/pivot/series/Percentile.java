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
import org.graylog.plugins.formatting.units.model.UnitId;
import org.graylog.plugins.views.search.searchtypes.pivot.HasField;
import org.graylog.plugins.views.search.searchtypes.pivot.MayHaveUnit;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(Percentile.NAME)
@JsonDeserialize(builder = Percentile.Builder.class)
public abstract class Percentile implements SeriesSpec, HasField, MayHaveUnit {
    public static final String NAME = "percentile";

    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @JsonProperty
    public abstract String field();

    @Override
    public abstract Optional<UnitId> unit();

    @JsonProperty
    public abstract Double percentile();

    @Override
    public String literal() {
        return type() + "(" + field() + "," + percentile() + ")";
    }

    public abstract Builder toBuilder();

    @Override
    public Percentile withId(String id) {
        return toBuilder().id(id).build();
    }

    public static Builder builder() {
        return new AutoValue_Percentile.Builder().type(NAME);
    }

    @AutoValue.Builder
    public abstract static class Builder extends SeriesSpecBuilder<Percentile, Builder> {
        @JsonCreator
        public static Builder create() {
            return Percentile.builder();
        }

        @Override
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder field(String field);

        @JsonProperty
        public abstract Builder percentile(Double percentile);

        @JsonProperty
        public abstract Builder unit(@Nullable UnitId unit);

        abstract Optional<String> id();
        abstract String field();
        abstract Double percentile();
        abstract Percentile autoBuild();

        @Override
        public Percentile build() {
            if (id().isEmpty()) {
                id(NAME + "(" + field() + "," + percentile().toString() + ")");
            }
            return autoBuild();
        }
    }
}
