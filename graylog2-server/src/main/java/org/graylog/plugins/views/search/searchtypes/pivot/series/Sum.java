package org.graylog.plugins.views.search.searchtypes.pivot.series;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(Sum.NAME)
@JsonDeserialize(builder = Sum.Builder.class)
public abstract class Sum implements SeriesSpec {
    public static final String NAME = "sum";

    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @JsonProperty
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_Sum.Builder().type(NAME);
    }

    @AutoValue.Builder
    public abstract static class Builder extends TypedBuilder<Sum, Builder> {
        @JsonCreator
        public static Builder create() { return Sum.builder(); }

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder field(String field);

        abstract Optional<String> id();
        abstract String field();
        abstract Sum autoBuild();

        public Sum build() {
            if (!id().isPresent()) {
                id(NAME + "(" + field() + ")");
            }
            return autoBuild();
        }
    }
}
