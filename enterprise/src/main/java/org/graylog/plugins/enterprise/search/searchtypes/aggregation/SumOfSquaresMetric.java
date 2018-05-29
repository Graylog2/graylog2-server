package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(SumOfSquaresMetric.NAME)
@JsonDeserialize(builder = SumOfSquaresMetric.Builder.class)
public abstract class SumOfSquaresMetric implements MetricSpec {
    public static final String NAME = "sumofsquares";

    @Override
    public abstract String type();

    @JsonProperty("field")
    public abstract String field();

    public static SumOfSquaresMetric.Builder builder() {
        return new AutoValue_SumOfSquaresMetric.Builder().type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() { return builder(); }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder field(String field);

        public abstract SumOfSquaresMetric build();
    }

    @AutoValue
    public abstract static class Result {

        @JsonProperty
        public abstract double sumOfSquares();

        @JsonCreator
        public static Result create(double sumOfSquares) {
            return new AutoValue_SumOfSquaresMetric_Result(sumOfSquares);
        }
    }}
