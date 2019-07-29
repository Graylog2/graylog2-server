package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class AggregationSeries {
    private static final String FIELD_ID = "id";
    private static final String FIELD_FUNCTION = "function";
    private static final String FIELD_FIELD = "field";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_FUNCTION)
    public abstract AggregationFunction function();

    @JsonProperty(FIELD_FIELD)
    public abstract Optional<String> field();

    public static Builder builder() {
        return new AutoValue_AggregationSeries.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder function(AggregationFunction function);

        public abstract Builder field(@Nullable String field);

        public abstract AggregationSeries build();
    }

    @JsonCreator
    public static AggregationSeries create(@JsonProperty(FIELD_ID) String id,
                                           @JsonProperty(FIELD_FUNCTION) AggregationFunction function,
                                           @JsonProperty(FIELD_FIELD) @Nullable String field) {
        return builder()
                .id(Optional.ofNullable(id).orElse(new ObjectId().toHexString()))
                .function(function)
                .field(field)
                .build();
    }
}
