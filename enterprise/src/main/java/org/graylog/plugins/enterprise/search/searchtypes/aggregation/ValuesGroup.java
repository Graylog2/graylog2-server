package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import java.util.Collections;
import java.util.List;

@AutoValue
@JsonTypeName(ValuesGroup.NAME)
@JsonDeserialize(builder = ValuesGroup.Builder.class)
public abstract class ValuesGroup implements GroupSpec {
    public static final String NAME = "values";

    public static ValuesGroup.Builder builder() {
        return new AutoValue_ValuesGroup.Builder()
                .type(NAME)
                .metrics(Collections.emptyList())
                .groups(Collections.emptyList());
    }

    @Override
    public abstract String type();

    @JsonProperty
    public abstract List<MetricSpec> metrics();

    @JsonProperty
    public abstract List<GroupSpec> groups();

    @JsonProperty
    public abstract String field();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return builder();
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder metrics(List<MetricSpec> metrics);

        @JsonProperty
        public abstract Builder groups(List<GroupSpec> groups);

        @JsonProperty
        public abstract Builder field(String field);

        public abstract ValuesGroup build();
    }

    @AutoValue
    public abstract static class Result {

        @JsonProperty
        public abstract List<Bucket> buckets();

        public static Result create(List<ValuesGroup.Bucket> buckets) {
            return new AutoValue_ValuesGroup_Result(buckets);
        }

    }

    @AutoValue
    public abstract static class Bucket {

        @JsonProperty
        public abstract String key();

        @JsonProperty
        public abstract List<Object> metrics();

        @JsonProperty
        public abstract List<Object> groups();

        @JsonProperty
        public abstract long count();

        public static Builder builder() {
            return new AutoValue_ValuesGroup_Bucket.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder key(String key);

            public abstract Builder metrics(List<Object> metrics);

            public abstract Builder groups(List<Object> groups);

            public abstract Builder count(long count);

            public abstract Bucket build();
        }
    }}
