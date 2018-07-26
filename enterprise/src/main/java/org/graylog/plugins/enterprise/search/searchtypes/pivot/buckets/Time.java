package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.TypedBuilder;

@AutoValue
@JsonTypeName(Time.NAME)
@JsonDeserialize(builder = Time.Builder.class)
public abstract class Time implements BucketSpec {
    public static final String NAME = "time";

    @Override
    public abstract String type();

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract Interval interval();

    public static Time.Builder builder() {
        return new AutoValue_Time.Builder()
                .type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<Time, Builder> {

        @JsonCreator
        public static Builder create() {
            return Time.builder();
        }

        @JsonProperty
        public abstract Builder field(String field);

        @JsonProperty
        public abstract Builder interval(Interval interval);
    }

}

