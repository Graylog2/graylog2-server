package org.graylog.plugins.views.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;

import javax.annotation.Nullable;

@AutoValue
@JsonTypeName(FieldMetric.NAME)
@JsonDeserialize(builder = FieldMetric.Builder.class)
public abstract class FieldMetric implements SearchType {
    public static final String NAME = "field_metric";

    public enum Operation {
        AVG, CARDINALITY, COUNT, MAX, MIN, SUM
    }

    @Override
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    public abstract String id();

    @Nullable
    @Override
    public abstract Filter filter();

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract Operation operation();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @Override
    public SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state) {
        return this;
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_FieldMetric.Builder();
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty
        public abstract Builder field(String field);

        @JsonProperty
        public abstract Builder operation(Operation operation);

        public abstract FieldMetric build();
    }

    @AutoValue
    public abstract static class DoubleResult implements SearchType.Result {
        @Override
        @JsonProperty
        public abstract String id();

        @Override
        @JsonProperty
        public String type() {
            return NAME;
        }

        @JsonProperty
        public abstract double value();

        public static Builder builder() {
            return new AutoValue_FieldMetric_DoubleResult.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(String id);

            public abstract Builder value(double value);

            public abstract DoubleResult build();
        }
    }

    @AutoValue
    public abstract static class LongResult implements SearchType.Result {
        @Override
        @JsonProperty
        public abstract String id();

        @Override
        @JsonProperty
        public String type() {
            return NAME;
        }

        @JsonProperty
        public abstract long value();

        public static Builder builder() {
            return new AutoValue_FieldMetric_LongResult.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(String id);

            public abstract Builder value(long value);

            public abstract LongResult build();
        }
    }
}
