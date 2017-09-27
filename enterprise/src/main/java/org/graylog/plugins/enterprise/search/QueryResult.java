package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_QueryResult.Builder.class)
public abstract class QueryResult {

    @JsonProperty
    public abstract Query query();

    @JsonProperty
    public abstract QueryJob queryJob();

    @JsonProperty
    public abstract Map<String, SearchType.Result> results();

    public static Builder builder() {
        return new AutoValue_QueryResult.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder query(Query query);

        public abstract Builder queryJob(QueryJob queryJob);

        public abstract Builder results(Map<String, SearchType.Result> results);

        public abstract QueryResult build();
    }
}
