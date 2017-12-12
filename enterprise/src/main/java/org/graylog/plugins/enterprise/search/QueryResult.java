package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_QueryResult.Builder.class)
public abstract class QueryResult {

    @JsonProperty
    public abstract Query query();

    @JsonProperty("search_types")
    public abstract Map<String, SearchType.Result> searchTypes();

    public static Builder builder() {
        return new AutoValue_QueryResult.Builder();
    }

    public static QueryResult emptyResult() {
        return builder().searchTypes(Collections.emptyMap()).query(Query.emptyRoot()).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder query(Query query);

        public abstract Builder searchTypes(Map<String, SearchType.Result> results);

        public abstract QueryResult build();
    }
}
