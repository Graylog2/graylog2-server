package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(AutoIntervalDTO.type)
@JsonDeserialize(builder = AutoIntervalDTO.Builder.class)
public abstract class AutoIntervalDTO implements IntervalDTO {
    public static final String type = "auto";
    private static final String FIELD_SCALING = "scaling";

    @JsonProperty
    public abstract String type();

    @JsonProperty(FIELD_SCALING)
    public abstract Optional<Double> scaling();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty(FIELD_SCALING)
        public abstract Builder scaling(@Nullable Double scaling);

        public abstract AutoIntervalDTO build();

        @JsonCreator
        static Builder builder() { return new AutoValue_AutoIntervalDTO.Builder().type(type); };
    }
}

