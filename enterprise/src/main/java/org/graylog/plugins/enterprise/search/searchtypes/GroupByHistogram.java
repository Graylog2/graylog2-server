package org.graylog.plugins.enterprise.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog2.indexer.searches.Searches;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonTypeName(GroupByHistogram.NAME)
@JsonDeserialize(builder = GroupByHistogram.Builder.class)
public abstract class GroupByHistogram implements SearchType {
    public static final String NAME = "group_by_histogram";

    private static final long DEFAULT_LIMIT = 5;
    private static final SortOrder DEFAULT_SORT_ORDER = SortOrder.DESC;
    private static final GroupBy.Operation DEFAULT_OPERATION = GroupBy.Operation.COUNT;

    @Override
    public abstract String type();

    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract List<String> fields();

    @Min(1)
    @JsonProperty
    public abstract long limit();

    @JsonProperty
    public abstract GroupBy.Operation operation();

    @JsonProperty
    public abstract SortOrder order();

    @Nullable
    @JsonProperty
    public abstract Searches.DateHistogramInterval interval();

    public static Builder builder() {
        return new AutoValue_GroupByHistogram.Builder()
                .type(NAME)
                .limit(DEFAULT_LIMIT)
                .order(DEFAULT_SORT_ORDER)
                .operation(DEFAULT_OPERATION);
    }

    public abstract Builder toBuilder();

    @Override
    public SearchType withId(String id) {
        return toBuilder().id(id).build();
    }

    @Override
    public SearchType applyExecutionContext(ObjectMapper objectMapper, Map<String, Object> state) {
        return this;
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder createDefault() {
            return GroupByHistogram.builder();
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder fields(List<String> fields);

        @JsonProperty
        public abstract Builder limit(long limit);

        @JsonProperty
        public abstract Builder operation(GroupBy.Operation operation);

        @JsonProperty
        public abstract Builder order(SortOrder order);

        @JsonProperty
        public abstract Builder interval(@Nullable Searches.DateHistogramInterval interval);

        public abstract GroupByHistogram build();
    }

    @AutoValue
    public abstract static class Result implements SearchType.Result {
        @Override
        @JsonProperty
        public abstract String id();

        @JsonProperty
        public String type() {
            return NAME;
        }

        @JsonProperty
        public abstract Map<Long, Bucket> buckets();

        public static Builder builder() {
            return new AutoValue_GroupByHistogram_Result.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(String id);

            public abstract Builder buckets(Map<Long, Bucket> buckets);

            public abstract Result build();
        }
    }

    @AutoValue
    public abstract static class Bucket {
        @JsonProperty
        public abstract List<GroupBy.Group> groups();

        public static Builder builder() {
            return new AutoValue_GroupByHistogram_Bucket.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder groups(List<GroupBy.Group> groups);

            public abstract Bucket build();
        }
    }
}