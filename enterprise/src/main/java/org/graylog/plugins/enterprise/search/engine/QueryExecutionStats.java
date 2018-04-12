package org.graylog.plugins.enterprise.search.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = QueryExecutionStats.Builder.class)
public abstract class QueryExecutionStats {
    @JsonProperty("duration")
    public abstract long duration();

    public static QueryExecutionStats empty() {
        return builder().build();
    }

    public static QueryExecutionStats create(long duration) {
        return builder()
                .duration(duration)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_QueryExecutionStats.Builder().duration(0L);
        }

        @JsonProperty("duration")
        public abstract Builder duration(long duration);

        public abstract QueryExecutionStats build();
    }
}