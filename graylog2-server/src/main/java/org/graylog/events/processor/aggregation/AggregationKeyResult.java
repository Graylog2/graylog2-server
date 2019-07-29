package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = AggregationKeyResult.Builder.class)
public abstract class AggregationKeyResult {
    public abstract ImmutableList<String> key();

    public abstract ImmutableList<AggregationSeriesValue> seriesValues();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationKeyResult.Builder();
        }

        public abstract Builder key(List<String> key);

        public abstract Builder seriesValues(List<AggregationSeriesValue> seriesValues);

        public abstract AggregationKeyResult build();
    }
}