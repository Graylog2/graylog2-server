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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = QueryDTO.Builder.class)
public abstract class QueryDTO {
    @JsonProperty
    public abstract Optional<String> id();

    @JsonProperty
    public abstract Optional<TimeRange> timerange();

    @JsonProperty
    public abstract Optional<Filter> filter();

    @JsonProperty
    public abstract List<UsedSearchFilter> filters();

    @JsonProperty
    public abstract Optional<BackendQuery> query();

    @Nonnull
    @JsonProperty("search_types")
    public abstract Set<SearchType> searchTypes();

    static QueryDTO fromQuery(Query query) {
        return QueryDTO.Builder.create()
                .id(query.id())
                .query(query.query())
                .filter(query.filter())
                .filters(query.filters())
                .searchTypes(query.searchTypes())
                .timerange(query.timerange())
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty
        public abstract Builder filters(List<UsedSearchFilter> usedSearchFilters);

        @JsonProperty
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty("search_types")
        public abstract Builder searchTypes(@Nonnull Set<SearchType> searchTypes);

        public abstract QueryDTO build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_QueryDTO.Builder()
                    .filters(Collections.emptyList());
        }
    }

    Query toQuery() {
        Query.Builder queryBuilder = Query.builder();
        queryBuilder = id().map(queryBuilder::id).orElse(queryBuilder);
        queryBuilder = timerange().map(queryBuilder::timerange).orElse(queryBuilder);
        queryBuilder = filter().map(queryBuilder::filter).orElse(queryBuilder);
        queryBuilder = query().map(queryBuilder::query).orElse(queryBuilder);

        return queryBuilder
                .searchTypes(ImmutableSet.copyOf(searchTypes()))
                .filters(filters())
                .build();
    }
}
