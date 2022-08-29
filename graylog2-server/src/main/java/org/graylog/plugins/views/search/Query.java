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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Traverser;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.EmptyTimeRange;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.ExecutionStateGlobalOverride;
import org.graylog.plugins.views.search.rest.SearchTypeExecutionState;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsesSearchFilters;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.QueryEntity;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableSortedSet.of;
import static java.util.stream.Collectors.toSet;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = Query.Builder.class)
public abstract class Query implements ContentPackable<QueryEntity>, UsesSearchFilters {

    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty
    public abstract Filter filter();

    @JsonProperty
    @Override
    public abstract List<UsedSearchFilter> filters();

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

    public Set<String> effectiveStreams(SearchType searchType) {
        return searchType.effectiveStreams().isEmpty()
                ? this.usedStreamIds()
                : searchType.effectiveStreams();
    }

    @Nonnull
    @JsonProperty("search_types")
    public abstract ImmutableSet<SearchType> searchTypes();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Query.Builder.createWithDefaults();
    }

    Query applyExecutionState(ExecutionStateGlobalOverride state) {
        if (state == null || !state.hasValues()) {
            return this;
        }

        if (state.timerange().isPresent() || state.query().isPresent() || !state.searchTypes().isEmpty() || !state.keepSearchTypes().isEmpty()) {
            final Builder builder = toBuilder();

            if (state.timerange().isPresent() || state.query().isPresent()) {
                final GlobalOverride.Builder globalOverrideBuilder = globalOverride().map(GlobalOverride::toBuilder)
                        .orElseGet(GlobalOverride::builder);
                state.timerange().ifPresent(timeRange -> {
                    globalOverrideBuilder.timerange(timeRange);
                    builder.timerange(timeRange);
                });

                state.query().ifPresent(query -> {
                    globalOverrideBuilder.query(query);
                    builder.query(query);
                });

                builder.globalOverride(globalOverrideBuilder.build());
            }

            if (!state.searchTypes().isEmpty() || !state.keepSearchTypes().isEmpty()) {
                final Set<SearchType> searchTypesToKeep = !state.keepSearchTypes().isEmpty()
                        ? filterForWhiteListFromState(searchTypes(), state)
                        : searchTypes();

                final Set<SearchType> searchTypesWithOverrides = applyAvailableOverrides(state, searchTypesToKeep);

                builder.searchTypes(ImmutableSet.copyOf(searchTypesWithOverrides));
            }
            return builder.build();
        }
        return this;
    }

    private Set<SearchType> filterForWhiteListFromState(Set<SearchType> previousSearchTypes, ExecutionStateGlobalOverride state) {
        return previousSearchTypes.stream()
                .filter(st -> state.keepSearchTypes().contains(st.id()))
                .collect(toSet());
    }

    private Set<SearchType> applyAvailableOverrides(ExecutionStateGlobalOverride state, Set<SearchType> searchTypes) {
        return searchTypes.stream().map(st -> {
            if (state.searchTypes().containsKey(st.id())) {
                final SearchTypeExecutionState executionState = state.searchTypes().get(st.id());
                return st.applyExecutionContext(executionState);
            } else {
                return st;
            }
        }).collect(toSet());
    }

    public static Query emptyRoot() {
        return Query.builder()
                .id("")
                .timerange(EmptyTimeRange.emptyTimeRange())
                .query(ElasticsearchQueryString.empty())
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

    public boolean hasReferencedStreamFilters() {
        return filters() != null && filters().stream().anyMatch(f -> f instanceof ReferencedSearchFilter);
    }

    Query addStreamsToFilter(Set<String> streamIds) {
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

        public abstract String id();

        @JsonProperty
        public abstract Builder timerange(TimeRange timerange);

        @JsonProperty
        public abstract Builder filter(Filter filter);

        @JsonProperty
        public abstract Builder filters(List<UsedSearchFilter> filters);

        @JsonProperty
        public abstract Builder query(BackendQuery query);

        public abstract Builder globalOverride(@Nullable GlobalOverride globalOverride);

        @JsonProperty("search_types")
        public abstract Builder searchTypes(@Nullable Set<SearchType> searchTypes);

        abstract Query autoBuild();

        @JsonCreator
        static Builder createWithDefaults() {
            try {
                return new AutoValue_Query.Builder()
                        .searchTypes(of())
                        .filters(Collections.emptyList())
                        .query(ElasticsearchQueryString.empty())
                        .timerange(RelativeRange.create(300));
            } catch (InvalidRangeParametersException e) {
                throw new RuntimeException("Unable to create relative timerange - this should not happen!");
            }
        }

        public Query build() {
            if (id() == null) {
                id(UUID.randomUUID().toString());
            }
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
                .filters(filters())
                .query(query())
                .id(id())
                .globalOverride(globalOverride().orElse(null))
                .timerange(timerange())
                .build();
    }
}
