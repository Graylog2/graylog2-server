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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Traverser;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.EmptyTimeRange;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.QueryEntity;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableSortedSet.of;
import static java.util.stream.Collectors.toSet;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = Query.Builder.class)
public abstract class Query implements ContentPackable<QueryEntity> {
    private static final Logger LOG = LoggerFactory.getLogger(Query.class);

    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty
    public abstract Filter filter();

    @Nonnull
    @JsonProperty
    public abstract BackendQuery query();

    @JsonIgnore
    public abstract Optional<GlobalOverride> globalOverride();

    public TimeRange effectiveTimeRange(SearchType searchType) {
        return searchType.timerange()
                .map(timeRange -> timeRange.effectiveTimeRange(this, searchType))
                .orElse(this.timerange());
    }

    @Nonnull
    @JsonProperty("search_types")
    public abstract ImmutableSet<SearchType> searchTypes();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Query.Builder.createWithDefaults();
    }

    Query applyExecutionState(ObjectMapper objectMapper, JsonNode state) {
        if (state.isMissingNode()) {
            return this;
        }
        final boolean hasTimerange = state.hasNonNull("timerange");
        final boolean hasQuery = state.hasNonNull("query");
        final boolean hasSearchTypes = state.hasNonNull("search_types");
        final boolean hasKeepSearchTypes = state.hasNonNull("keep_search_types");
        if (hasTimerange || hasQuery || hasSearchTypes || hasKeepSearchTypes) {
            final Builder builder = toBuilder();

            if (hasTimerange || hasQuery) {
                final GlobalOverride.Builder globalOverrideBuilder = globalOverride().map(GlobalOverride::toBuilder)
                        .orElseGet(GlobalOverride::builder);
                if (hasTimerange) {
                    try {
                        final Object rawTimerange = state.path("timerange");
                        final TimeRange newTimeRange = objectMapper.convertValue(rawTimerange, TimeRange.class);
                        globalOverrideBuilder.timerange(newTimeRange);
                        builder.timerange(newTimeRange);
                    } catch (Exception e) {
                        LOG.error("Unable to deserialize execution state for time range", e);
                    }
                }
                if (hasQuery) {
                    final Object rawQuery = state.path("query");
                    final BackendQuery newQuery = objectMapper.convertValue(rawQuery, BackendQuery.class);
                    globalOverrideBuilder.query(newQuery);
                    builder.query(newQuery);
                }
                builder.globalOverride(globalOverrideBuilder.build());
            }

            if (hasSearchTypes || hasKeepSearchTypes) {
                final Set<SearchType> searchTypesToKeep = hasKeepSearchTypes
                        ? filterForWhiteListFromState(searchTypes(), state)
                        : searchTypes();

                final Set<SearchType> searchTypesWithOverrides = applyAvailableOverrides(objectMapper, state, searchTypesToKeep);

                builder.searchTypes(ImmutableSet.copyOf(searchTypesWithOverrides));
            }
            return builder.build();
        }
        return this;
    }

    private Set<SearchType> filterForWhiteListFromState(Set<SearchType> previousSearchTypes, JsonNode state) {
        final Set<String> whitelist = parseSearchTypesWhitelistFrom(state);

        return previousSearchTypes.stream()
                .filter(st -> whitelist.contains(st.id()))
                .collect(toSet());
    }

    private Set<String> parseSearchTypesWhitelistFrom(JsonNode state) {
        final String key = "keep_search_types";
        final Set<String> results = new HashSet<>();
        if (state.has(key) && state.get(key).isArray()) {
            for (JsonNode n : state.get(key)) {
                results.add(n.asText());
            }
        }
        return results;
    }

    private Set<SearchType> applyAvailableOverrides(ObjectMapper objectMapper, JsonNode state, Set<SearchType> searchTypes) {
        final JsonNode searchTypesState = state.path("search_types");

        return searchTypes.stream().map(st -> {
            if (searchTypesState.has(st.id())) {
                return st.applyExecutionContext(objectMapper, searchTypesState.path(st.id()));
            } else {
                return st;
            }
        }).collect(toSet());
    }

    public static Query emptyRoot() {
        return Query.builder()
                .id("")
                .timerange(EmptyTimeRange.emptyTimeRange())
                .query(new BackendQuery.Fallback())
                .filter(null)
                .build();
    }

    @SuppressWarnings("UnstableApiUsage")
    public Set<String> usedStreamIds() {
        return Optional.ofNullable(filter())
                .map(optFilter -> {
                    final Traverser<Filter> filterTraverser = Traverser.forTree(filter -> firstNonNull(filter.filters(), Collections.emptySet()));
                    return StreamSupport.stream(filterTraverser.breadthFirst(optFilter).spliterator(), false)
                            .filter(filter -> filter instanceof StreamFilter)
                            .map(streamFilter -> ((StreamFilter) streamFilter).streamId())
                            .filter(Objects::nonNull)
                            .collect(toSet());
                })
                .orElse(Collections.emptySet());
    }

    public boolean hasStreams() {
        return !usedStreamIds().isEmpty();
    }

    Query addStreamsToFilter(ImmutableSet<String> streamIds) {
        final Filter newFilter = addStreamsTo(filter(), streamIds);
        return toBuilder().filter(newFilter).build();
    }

    private Filter addStreamsTo(Filter filter, Set<String> streamIds) {
        final Filter streamIdFilter = StreamFilter.anyIdOf(streamIds.toArray(new String[]{}));
        if (filter == null) {
            return streamIdFilter;
        }
        return AndFilter.and(streamIdFilter, filter);
    }

    public boolean hasSearchType(String searchTypeId) {
        return searchTypes().stream()
                .map(SearchType::id)
                .anyMatch(id -> id.equals(searchTypeId));
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder timerange(TimeRange timerange);

        @JsonProperty
        public abstract Builder filter(Filter filter);

        @JsonProperty
        public abstract Builder query(BackendQuery query);

        public abstract Builder globalOverride(@Nullable GlobalOverride globalOverride);

        @JsonProperty("search_types")
        public abstract Builder searchTypes(@Nullable Set<SearchType> searchTypes);

        abstract Query autoBuild();

        @JsonCreator
        static Builder createWithDefaults() {
            return new AutoValue_Query.Builder().searchTypes(of());
        }

        public Query build() {
            return autoBuild();
        }
    }

    // TODO: This code assumes that we only use shallow filters for streams.
    //       If this ever changes, we need to implement a mapper that can handle filter trees.
    private Filter shallowMappedFilter(EntityDescriptorIds entityDescriptorIds) {
        return Optional.ofNullable(filter())
                .map(optFilter -> {
                    Set<Filter> newFilters = optFilter.filters().stream()
                            .map(filter -> {
                                if (filter.type().equals(StreamFilter.NAME)) {
                                    final StreamFilter streamFilter = (StreamFilter) filter;
                                    final String streamId = entityDescriptorIds.
                                            getOrThrow(streamFilter.streamId(), ModelTypes.STREAM_V1);
                                    return streamFilter.toBuilder().streamId(streamId).build();
                                }
                                return filter;
                            }).collect(toSet());
                    return optFilter.toGenericBuilder().filters(newFilters).build();
                })
                .orElse(null);
    }

    @Override
    public QueryEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return QueryEntity.builder()
                .searchTypes(searchTypes().stream().map(s -> s.toContentPackEntity(entityDescriptorIds))
                        .collect(Collectors.toSet()))
                .filter(shallowMappedFilter(entityDescriptorIds))
                .query(query())
                .id(id())
                .globalOverride(globalOverride().orElse(null))
                .timerange(timerange())
                .build();
    }
}
