package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

@AutoValue
@JsonTypeName(Values.NAME)
@JsonDeserialize(builder = Values.Builder.class)
public abstract class Values implements BucketSpec {
    public static final String NAME = "values";
    private static final int DEFAULT_LIMIT = 5;

    @Override
    public abstract String type();

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract int limit();

    public static Values.Builder builder() {
        return new AutoValue_Values.Builder()
                .type(NAME)
                .limit(DEFAULT_LIMIT);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<Values, Builder> {

        @JsonCreator
        public static Builder create() {
            return Values.builder();
        }

        @JsonProperty
        public abstract Builder field(String field);

        @JsonProperty
        public abstract Builder limit(int limit);

    }

}

