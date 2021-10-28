package org.graylog.plugins.views.search.searchtypes.pivot.series;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import java.util.Optional;

@AutoValue
@JsonTypeName(Latest.NAME)
@JsonDeserialize(builder = Latest.Builder.class)
public abstract class Latest implements SeriesSpec {
    public static final String NAME = "latest";
    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @JsonProperty
    public abstract String field();

    public static Latest.Builder builder() {
        return new AutoValue_Latest.Builder().type(NAME);
    }

    @AutoValue.Builder
    public abstract static class Builder extends TypedBuilder<Latest, Latest.Builder> {
        @JsonCreator
        public static Builder create() { return Latest.builder(); }

        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder field(String field);

        abstract Optional<String> id();
        abstract String field();
        abstract Latest autoBuild();

        public Latest build() {
            if (!id().isPresent()) {
                id(NAME + "(" + field() + ")");
            }
            return autoBuild();
        }
    }
}
