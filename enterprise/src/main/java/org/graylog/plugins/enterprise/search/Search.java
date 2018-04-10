package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_Search.Builder.class)
public abstract class Search {

    // generated during build to help quickly find a query by id.
    private ImmutableMap<String, Query> queryIndex;

    // generated during build to help quickly find a parameter by name.
    private ImmutableMap<String, Parameter> parameterIndex;

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract ImmutableSet<Query> queries();

    @JsonProperty
    public abstract ImmutableSet<Parameter> parameters();

    public Search applyExecutionState(ObjectMapper objectMapper, Map<String, Object> executionState) {
        //noinspection unchecked
        final ImmutableSet<Query> queries = queries().stream()
                .map(query -> query.applyExecutionState(objectMapper, (Map<String, Object>) executionState.get(query.id())))
                .collect(ImmutableSet.toImmutableSet());
        return toBuilder().queries(queries).build();
    }

    abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Search.Builder().parameters(of());
    }

    @JsonIgnore
    public Optional<Query> getQuery(String sourceQueryId) {
        return Optional.ofNullable(queryIndex.get(sourceQueryId));
    }

    @JsonIgnore
    public Optional<Parameter> getParameter(String parameterName) {
        return Optional.ofNullable(parameterIndex.get(parameterName));
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        @Id
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder queries(ImmutableSet<Query> queries);

        @JsonProperty
        public abstract Builder parameters(ImmutableSet<Parameter> parameters);

        abstract Optional<ImmutableSet<Parameter>> parameters();

        abstract Search autoBuild();

        public Search build() {
            if (!parameters().isPresent()) {
                parameters(of());
            }
            final Search search = autoBuild();
            search.queryIndex = Maps.uniqueIndex(search.queries(), Query::id);
            search.parameterIndex = Maps.uniqueIndex(search.parameters(), Parameter::name);
            return search;
        }

    }
}
