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
package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.processor.aggregation.AggregationFunction;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = AggregationSeriesEntity.Builder.class)
public abstract class AggregationSeriesEntity {

    private static final String FIELD_FUNCTION = "function";
    private static final String FIELD_FIELD = "field";

    @JsonProperty(FIELD_FUNCTION)
    public abstract AggregationFunction function();

    @Nullable
    @JsonProperty(FIELD_FIELD)
    public abstract Optional<String> field();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationSeriesEntity.Builder();
        }

        @JsonProperty(FIELD_FUNCTION)
        public abstract Builder function(AggregationFunction function);

        @JsonProperty(FIELD_FIELD)
        public abstract Builder field(@Nullable String field);

        public abstract AggregationSeriesEntity build();
    }
}
