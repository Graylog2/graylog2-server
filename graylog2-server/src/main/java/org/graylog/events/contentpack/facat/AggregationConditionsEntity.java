package org.graylog.events.contentpack.facat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AggregationConditionsEntity.Builder.class)
public abstract class AggregationConditionsEntity {

    private static final String FIELD_EXPRESSION = "expression";

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationConditionsEntity.Builder();
        }

        public abstract AggregationConditionsEntity build();
    }
}