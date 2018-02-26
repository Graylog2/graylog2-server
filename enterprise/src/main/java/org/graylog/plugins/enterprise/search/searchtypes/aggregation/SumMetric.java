package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(SumMetric.NAME)
@JsonDeserialize(builder = SumMetric.Builder.class)
public abstract class SumMetric implements MetricSpec {
    public static final String NAME = "sum";

    @Override
    public abstract String type();

    @JsonProperty("field")
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_SumMetric.Builder().type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        public static SumMetric.Builder create() { return builder(); }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder field(String field);

        public abstract SumMetric build();
    }

    @AutoValue
    public abstract static class Result {

        @JsonProperty
        public abstract double sum();

        @JsonCreator
        public static Result create(double sum) {
            return new AutoValue_SumMetric_Result(sum);
        }
    }
}
