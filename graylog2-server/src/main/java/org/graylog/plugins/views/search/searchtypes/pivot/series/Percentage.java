package org.graylog.plugins.views.search.searchtypes.pivot.series;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;

import java.util.Optional;

@AutoValue
@JsonTypeName(Percentage.NAME)
@JsonDeserialize(builder = Percentage.Builder.class)
public abstract class Percentage implements SeriesSpec {
    public static final String NAME = "percentage";

    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends SeriesSpecBuilder<Percentage, Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_Percentage.Builder();
        }

        @Override
        @JsonProperty
        public abstract Builder id(String id);

        abstract Optional<String> id();

        abstract Percentage autoBuild();

        @Override
        public Percentage build() {
            if (id().isEmpty()) {
                id(NAME + "()");
            }
            return autoBuild();
        }
    }
}
