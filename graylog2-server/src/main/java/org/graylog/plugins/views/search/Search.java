/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.SearchEntity;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
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
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toSet;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = Search.Builder.class)
public abstract class Search implements ContentPackable<SearchEntity> {
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

        if (defaultStreams.isEmpty()) {
            throw new MissingStreamPermissionException("User doesn't have access to any streams",
                    Collections.emptySet());
        }

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

    public Set<String> usedStreamIds() {
        final Set<String> queryStreamIds = queries().stream()
                .map(Query::usedStreamIds)
                .reduce(Collections.emptySet(), Sets::union);
        final Set<String> searchTypeStreamIds = queries().stream()
                .flatMap(q -> q.searchTypes().stream())
                .map(SearchType::effectiveStreams)
                .reduce(Collections.emptySet(), Sets::union);

        return Sets.union(queryStreamIds, searchTypeStreamIds);
    }

    public Set<String> streamIdsForPermissionsCheck() {
        final Set<String> queryStreamIds = queries().stream()
                .map(Query::usedStreamIds)
                .reduce(Collections.emptySet(), Sets::union);
        final Set<String> searchTypeStreamIds = queries().stream()
                .flatMap(q -> q.searchTypes().stream())
                .map(SearchType::streams)
                .reduce(Collections.emptySet(), Sets::union);

        return Sets.union(queryStreamIds, searchTypeStreamIds);
    }

    public Query queryForSearchType(String searchTypeId) {
        return queries().stream()
                .filter(q -> q.hasSearchType(searchTypeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Search " + id() + " doesn't have a query for search type " + searchTypeId));
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
        public abstract Builder owner(@Nullable String owner);

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

    @Override
    public SearchEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        final SearchEntity.Builder searchEntityBuilder = SearchEntity.builder()
                .queries(ImmutableSet.copyOf(this.queries().stream()
                        .map(query -> query.toContentPackEntity(entityDescriptorIds))
                        .collect(Collectors.toSet())))
                .parameters(this.parameters())
                .requires(this.requires())
                .createdAt(this.createdAt());
        if (this.owner().isPresent()) {
            searchEntityBuilder.owner(this.owner().get());
        }
        return searchEntityBuilder.build();
    }
}
