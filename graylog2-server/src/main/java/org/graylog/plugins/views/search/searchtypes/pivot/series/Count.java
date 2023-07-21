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
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(Count.NAME)
@JsonDeserialize(builder = Count.Builder.class)
public abstract class Count implements SeriesSpec, HasOptionalField {
    public static final String NAME = "count";

    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @JsonProperty
    public abstract Optional<String> field();

    @Override
    public String literal() {
        return type() + "(" + field().orElse("") + ")";
    }

    public abstract Builder toBuilder();

    @Override
    public Count withId(String id) {
        return toBuilder().id(id).build();
    }

    public static Builder builder() {
        return new AutoValue_Count.Builder().type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends SeriesSpecBuilder<Count, Builder> {
        @JsonCreator
        public static Builder create() {
            return Count.builder();
        }

        @Override
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder field(@Nullable String field);

        abstract Optional<String> id();

        abstract Optional<String> field();

        abstract Count autoBuild();

        @Override
        public Count build() {
            if (id().isEmpty()) {
                id(NAME + "(" + field().orElse("") + ")");
            }
            return autoBuild();
        }
    }
}
