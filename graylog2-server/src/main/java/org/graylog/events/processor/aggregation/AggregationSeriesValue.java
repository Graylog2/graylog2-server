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
package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = AggregationSeriesValue.Builder.class)
public abstract class AggregationSeriesValue {
    public abstract AggregationSeries series();

    public abstract List<String> key();

    public abstract double value();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationSeriesValue.Builder();
        }

        public abstract Builder series(AggregationSeries series);

        public abstract Builder key(List<String> key);

        public abstract Builder value(double value);

        public abstract AggregationSeriesValue build();
    }
}
