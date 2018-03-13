package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonTypeName(CountMetric.NAME)
@JsonDeserialize(builder = CountMetric.Builder.class)
public abstract class CountMetric implements MetricSpec {
    public static final String NAME = "count";

    @Override
    public abstract String type();

    @Nullable
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_CountMetric.Builder().type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        public static CountMetric.Builder create() { return builder(); }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder field(@Nullable String field);

        public abstract CountMetric build();
    }

    @AutoValue
    public abstract static class Result {

        @JsonProperty("count")
        public abstract long count();

        @JsonCreator
        public static Result create(@JsonProperty("count") long count) {
            return new AutoValue_CountMetric_Result(count);
        }
    }
}
