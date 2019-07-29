package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.conditions.Expression;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = AggregationConditions.Builder.class)
public abstract class AggregationConditions {
    private static final String FIELD_EXPRESSION = "expression";

    @JsonProperty(FIELD_EXPRESSION)
    public abstract Optional<Expression<Boolean>> expression();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationConditions.Builder();
        }

        @JsonProperty(FIELD_EXPRESSION)
        public abstract Builder expression(@Nullable Expression<Boolean> expression);

        public abstract AggregationConditions build();
    }
}