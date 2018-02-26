package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Iterables;
import org.graylog.plugins.enterprise.search.SearchType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonTypeName(Aggregation.NAME)
@JsonDeserialize(builder = Aggregation.Builder.class)
public abstract class Aggregation implements SearchType {
    public static final String NAME = "aggregation";

    public static Builder builder() {
        return new AutoValue_Aggregation.Builder()
                .groups(Collections.emptyList())
                .metrics(Collections.emptyList());
    }

    @Override
    public abstract String type();

    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract List<MetricSpec> metrics();

    @JsonProperty
    public abstract List<GroupSpec> groups();

    @JsonIgnore
    public Iterable<AggregationSpec> aggregations() {
        return Iterables.concat(groups(), metrics());
    }

    @Override
    public SearchType withId(String id) {
        return toBuilder().id(id).build();
    }

    @Override
    public SearchType applyExecutionContext(ObjectMapper objectMapper, Map<String, Object> state) {
        return this;
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder createDefault() {
            return builder();
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder metrics(List<MetricSpec> metrics);

        @JsonProperty
        public abstract Builder groups(List<GroupSpec> groups);

        public abstract Aggregation build();
    }

    @AutoValue
    public abstract static class Result implements SearchType.Result {
        public static Builder builder() {
            return new AutoValue_Aggregation_Result.Builder();
        }

        @Override
        @JsonProperty
        public abstract String id();

        @JsonProperty
        public String type() {
            return NAME;
        }

        @JsonProperty
        public abstract List<Object> metrics();

        @JsonProperty
        public abstract List<Object> groups();

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(String id);

            public abstract Builder metrics(List<Object> metrics);

            public abstract Builder groups(List<Object> groups);

            public abstract Result build();
        }
    }
}
