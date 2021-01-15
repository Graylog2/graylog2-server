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
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(Count.NAME)
@JsonDeserialize(builder = Count.Builder.class)
public abstract class Count implements SeriesSpec {
    public static final String NAME = "count";
    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @Nullable
    @JsonProperty
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_Count.Builder().type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<Count, Builder> {
        @JsonCreator
        public static Builder create() {
            return Count.builder();
        }

        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder field(@Nullable String field);

        abstract Optional<String> id();
        abstract String field();
        abstract Count autoBuild();

        public Count build() {
            if (!id().isPresent()) {
                id(NAME + "(" + Strings.nullToEmpty(field()) + ")");
            }
            return autoBuild();
        }
    }
}
