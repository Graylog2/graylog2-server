package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.engine.QueryExecutionStats;

import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_QueryResult.Builder.class)
public abstract class QueryResult {

    @JsonProperty
    public abstract Query query();

    @JsonProperty("execution_stats")
    public abstract QueryExecutionStats executionStats();

    @JsonProperty("search_types")
    public abstract Map<String, SearchType.Result> searchTypes();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static QueryResult emptyResult() {
        return builder().searchTypes(Collections.emptyMap()).query(Query.emptyRoot()).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_QueryResult.Builder()
                    .executionStats(QueryExecutionStats.empty());
        }

        public abstract Builder query(Query query);

        public abstract Builder executionStats(QueryExecutionStats stats);

        public abstract Builder searchTypes(Map<String, SearchType.Result> results);

        public abstract QueryResult build();
    }
}
