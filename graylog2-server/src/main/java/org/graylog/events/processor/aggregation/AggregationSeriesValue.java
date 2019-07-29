package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = AggregationSeriesValue.Builder.class)
public abstract class AggregationSeriesValue {
    public abstract AggregationSeries series();

    public abstract ImmutableList<String> key();

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