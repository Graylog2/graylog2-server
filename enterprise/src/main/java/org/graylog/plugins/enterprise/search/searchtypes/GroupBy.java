package org.graylog.plugins.enterprise.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.Filter;
import org.graylog.plugins.enterprise.search.SearchType;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import java.util.List;

@AutoValue
@JsonTypeName(GroupBy.NAME)
@JsonDeserialize(builder = GroupBy.Builder.class)
public abstract class GroupBy implements SearchType {
    public static final String NAME = "group_by";

    public enum Operation {
        COUNT
    }

    private static final long DEFAULT_LIMIT = 5;
    private static final SortOrder DEFAULT_SORT_ORDER = SortOrder.DESC;
    private static final Operation DEFAULT_OPERATION = Operation.COUNT;

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
    public abstract List<String> fields();

    @Min(1)
    @JsonProperty
    public abstract long limit();

    @JsonProperty
    public abstract Operation operation();

    @JsonProperty
    public abstract SortOrder order();

    public static Builder builder() {
        return new AutoValue_GroupBy.Builder()
                .type(NAME)
                .limit(DEFAULT_LIMIT)
                .order(DEFAULT_SORT_ORDER)
                .operation(DEFAULT_OPERATION);
    }

    public abstract Builder toBuilder();

    @Override
    public SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state) {
        return this;
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder createDefault() {
            return GroupBy.builder();
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty
        public abstract Builder fields(List<String> fields);

        @JsonProperty
        public abstract Builder limit(long limit);

        @JsonProperty
        public abstract Builder operation(Operation operation);

        @JsonProperty
        public abstract Builder order(SortOrder order);

        public abstract GroupBy build();
    }

    @AutoValue
    public abstract static class Result implements SearchType.Result {
        @Override
        @JsonProperty
        public abstract String id();

        @Override
        @JsonProperty
        public String type() {
            return NAME;
        }

        @JsonProperty
        public abstract List<Group> groups();

        public static Builder builder() {
            return new AutoValue_GroupBy_Result.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(String id);

            public abstract Builder groups(List<Group> groups);

            public abstract Result build();
        }
    }

    @AutoValue
    public abstract static class Group {
        @JsonProperty
        public abstract long count();

        @JsonProperty
        public abstract List<GroupField> fields();

        public static Builder builder() {
            return new AutoValue_GroupBy_Group.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty
            public abstract Builder count(long count);

            @JsonProperty
            public abstract Builder fields(List<GroupField> fields);

            public abstract Group build();
        }
    }

    @AutoValue
    public abstract static class GroupField {
        @JsonProperty
        public abstract String field();

        @JsonProperty
        public abstract String value();

        public static Builder builder() {
            return new AutoValue_GroupBy_GroupField.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty
            public abstract Builder field(String field);

            @JsonProperty
            public abstract Builder value(String value);

            public abstract GroupField build();
        }
    }
}