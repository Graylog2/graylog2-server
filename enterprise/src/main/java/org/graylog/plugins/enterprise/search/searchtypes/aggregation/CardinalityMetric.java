package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(CardinalityMetric.NAME)
@JsonDeserialize(builder = CardinalityMetric.Builder.class)
public abstract class CardinalityMetric implements MetricSpec {
    public static final String NAME = "card";

    @Override
    public abstract String type();

    @JsonProperty("field")
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_CardinalityMetric.Builder().type(NAME);
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

        public abstract CardinalityMetric build();
    }

    @AutoValue
    public abstract static class Result {

        @JsonProperty
        public abstract long cardinality();

        @JsonCreator
        public static Result create(long cardinality) {
            return new AutoValue_CardinalityMetric_Result(cardinality);
        }
    }
}
