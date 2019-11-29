/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toSet;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = Search.Builder.class)
public abstract class Search {
    public static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_OWNER = "owner";

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

    @JsonProperty(FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    @JsonIgnore
    public Optional<Query> getQuery(String sourceQueryId) {
        return Optional.ofNullable(queryIndex.get(sourceQueryId));
    }

    @JsonIgnore
    public Optional<Parameter> getParameter(String parameterName) {
        return Optional.ofNullable(parameterIndex.get(parameterName));
    }

    @SuppressWarnings("UnstableApiUsage")
    public Search applyExecutionState(ObjectMapper objectMapper, Map<String, Object> executionState) {
        final Builder builder = toBuilder();

        final JsonNode state = objectMapper.convertValue(executionState, JsonNode.class);

        if (state.hasNonNull("parameter_bindings")) {
            final ImmutableSet<Parameter> parameters = parameters().stream()
                    .map(param -> param.applyExecutionState(objectMapper, state.path("parameter_bindings")))
                    .collect(toImmutableSet());
            builder.parameters(parameters);
        }
        if (state.hasNonNull("queries") || state.hasNonNull("global_override")) {
            final ImmutableSet<Query> queries = queries().stream()
                    .map(query -> {
                        final JsonNode queryOverride = state.hasNonNull("global_override")
                                ? state.path("global_override")
                                : state.path("queries").path(query.id());
                        return query.applyExecutionState(objectMapper, queryOverride);
                    })
                    .filter(Query::hasSearchTypes)
                    .collect(toImmutableSet());
            builder.queries(queries);
        }
        return builder.build();
    }

    public Search addStreamsToQueriesWithoutStreams(Supplier<ImmutableSet<String>> defaultStreamsSupplier) {
        if (!hasQueriesWithoutStreams()) {
            return this;
        }
        final Set<Query> withStreams = queries().stream().filter(Query::hasStreams).collect(toSet());
        final Set<Query> withoutStreams = Sets.difference(queries(), withStreams);

        final ImmutableSet<String> defaultStreams = defaultStreamsSupplier.get();
        final Set<Query> withDefaultStreams = withoutStreams.stream()
                .map(q -> q.addStreamsToFilter(defaultStreams))
                .collect(toSet());

        final ImmutableSet<Query> newQueries = Sets.union(withStreams, withDefaultStreams).immutableCopy();

        return toBuilder().queries(newQueries).build();
    }

    private boolean hasQueriesWithoutStreams() {
        return !queries().stream().allMatch(Query::hasStreams);
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create().parameters(of()).queries(ImmutableSet.<Query>builder().build());
    }

    Set<String> usedStreamIds() {
        final Set<String> queryStreamIds = queries().stream()
                .map(Query::usedStreamIds)
                .reduce(Collections.emptySet(), Sets::union);
        final Set<String> searchTypeStreamIds = queries().stream()
                .flatMap(q -> q.searchTypes().stream())
                .map(SearchType::effectiveStreams)
                .reduce(Collections.emptySet(), Sets::union);

        return Sets.union(queryStreamIds, searchTypeStreamIds);
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

        @JsonProperty(FIELD_REQUIRES)
        public abstract Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        abstract Search autoBuild();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_Search.Builder()
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC))
                    .parameters(of());
        }

        public Search build() {
            final Search search = autoBuild();
            search.queryIndex = Maps.uniqueIndex(search.queries(), Query::id);
            search.parameterIndex = Maps.uniqueIndex(search.parameters(), Parameter::name);
            return search;
        }

    }
}
