package org.graylog.plugins.enterprise.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.ExecutionState;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog2.indexer.searches.Searches;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonTypeName(DateHistogram.NAME)
@JsonDeserialize(builder = AutoValue_DateHistogram.Builder.class)
public abstract class DateHistogram implements SearchType {
    public static final String NAME = "date_histogram";

    @Override
    @JsonProperty
    public abstract String type();

    @Nullable
    @JsonProperty
    public abstract String id();

    @ExecutionState
    @JsonProperty
    public abstract Searches.DateHistogramInterval interval();

    @Override
    public SearchType withId(String id) {
        return toBuilder().id(id).build();
    }

    abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_DateHistogram.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder interval(Searches.DateHistogramInterval interval);

        public abstract DateHistogram build();
    }

    @AutoValue
    public abstract static class Result implements SearchType.Result {

        @Override
        @JsonProperty
        public abstract String id();

        @JsonProperty
        public abstract Map<Long, Long> results();

        public static DateHistogram.Result.Builder builder() {
            return new AutoValue_DateHistogram_Result.Builder();
        }

        public static DateHistogram.Result.Builder result(String searchTypeId) {
            return builder().id(searchTypeId);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract DateHistogram.Result.Builder id(String id);

            public abstract Builder results(Map<Long, Long> results);

            public abstract DateHistogram.Result build();
        }
    }
}
