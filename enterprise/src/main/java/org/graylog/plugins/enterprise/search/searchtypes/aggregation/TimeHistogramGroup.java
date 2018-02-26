package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

@AutoValue
@JsonTypeName(TimeHistogramGroup.NAME)
@JsonDeserialize(builder = TimeHistogramGroup.Builder.class)
public abstract class TimeHistogramGroup implements GroupSpec {
    public static final String NAME = "time";

    public static Builder builder() {
        return new AutoValue_TimeHistogramGroup.Builder()
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

    @JsonProperty
    public abstract String interval();

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

        @JsonProperty
        public abstract Builder interval(String interval);

        public abstract TimeHistogramGroup build();
    }

    @AutoValue
    public abstract static class Result {

        @JsonProperty
        public abstract List<Bucket> buckets();

        @JsonProperty
        public abstract AbsoluteRange timerange();

        public static Result create(List<Bucket> buckets, AbsoluteRange timerange) {
            return new AutoValue_TimeHistogramGroup_Result(buckets, timerange);
        }

    }

    @AutoValue
    public abstract static class Bucket {

        @JsonProperty
        public abstract DateTime key();

        @JsonProperty
        public abstract List<Object> metrics();

        @JsonProperty
        public abstract List<Object> groups();

        public static Builder builder() {
            return new AutoValue_TimeHistogramGroup_Bucket.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder key(DateTime key);

            public abstract Builder metrics(List<Object> metrics);

            public abstract Builder groups(List<Object> groups);

            public abstract Bucket build();
        }
    }
}
