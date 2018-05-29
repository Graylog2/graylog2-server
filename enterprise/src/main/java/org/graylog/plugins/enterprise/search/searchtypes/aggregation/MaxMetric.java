package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(MaxMetric.NAME)
@JsonDeserialize(builder = MaxMetric.Builder.class)
public abstract class MaxMetric implements MetricSpec {
    public static final String NAME = "max";

    @Override
    public abstract String type();

    @JsonProperty("field")
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_MaxMetric.Builder().type(NAME);
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

        public abstract MaxMetric build();
    }

    @AutoValue
    public abstract static class Result {

        @JsonProperty
        public abstract double max();

        @JsonCreator
        public static Result create(double max) {
            return new AutoValue_MaxMetric_Result(max);
        }
    }
}
