package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_Search.Builder.class)
public abstract class Search {

    // generated during build to help quickly find a query by id.
    private ImmutableMap<String, Query> queryIndex;

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract ImmutableSet<Query> queries();

    public Search applyExecutionState(ObjectMapper objectMapper, Map<String, Object> executionState) {
        //noinspection unchecked
        final ImmutableSet<Query> queries = queries().stream()
                .map(query -> query.applyExecutionState(objectMapper, (Map<String, Object>) executionState.get(query.id())))
                .collect(ImmutableSet.toImmutableSet());
        return toBuilder().queries(queries).build();
    }

    abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Search.Builder();
    }

    public Query getQuery(String sourceQueryId) {
        return queryIndex.get(sourceQueryId);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @Id
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder queries(ImmutableSet<Query> queries);

        abstract Search autoBuild();

        public Search build() {
            final Search search = autoBuild();
            search.queryIndex = Maps.uniqueIndex(search.queries(), Query::id);
            return search;
        }

    }
}
