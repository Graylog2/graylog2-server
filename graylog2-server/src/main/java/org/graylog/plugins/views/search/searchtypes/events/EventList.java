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
package org.graylog.plugins.views.search.searchtypes.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.SearchTypeBuilder;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.rest.SearchTypeExecutionState;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.EventListEntity;
import org.graylog2.contentpacks.model.entities.SearchTypeEntity;
import org.graylog2.database.filtering.AttributeFilter;
import org.graylog2.plugin.Message;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

@AutoValue
@JsonTypeName(EventList.NAME)
@JsonDeserialize(builder = EventList.Builder.class)
public abstract class EventList implements SearchType {
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String NAME = "events";
    public static final Set<String> KNOWN_ATTRIBUTES = Set.of("priority", "event_definition_id", "alert");
    public static final SortConfig DEFAULT_SORT = new SortConfig(Message.FIELD_TIMESTAMP, Direction.DESC);

    public enum Direction {
        ASC,
        DESC;
    }

    public record SortConfig(@JsonProperty("field") String field, @JsonProperty("direction") Direction direction) {}

    @Override
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    public abstract String id();

    @Nullable
    @Override
    public abstract Filter filter();

    @Override
    @JsonProperty(FIELD_SEARCH_FILTERS)
    public abstract List<UsedSearchFilter> filters();

    @JsonProperty
    public abstract Optional<Integer> page();

    @JsonProperty
    public abstract Optional<Integer> perPage();

    @JsonProperty
    public abstract List<AttributeFilter> attributes();

    @JsonProperty
    public abstract Optional<SortConfig> sort();

    public SortConfig sortWithDefault() {
        return sort().orElse(DEFAULT_SORT);
    }

    @JsonCreator
    public static Builder builder() {
        return new AutoValue_EventList.Builder()
                .type(NAME)
                .filters(Collections.emptyList())
                .streams(Collections.emptySet())
                .attributes(Collections.emptyList());
    }

    public abstract Builder toBuilder();

    @Override
    public SearchType applyExecutionContext(SearchTypeExecutionState state) {
        if (state.page().isPresent() || state.perPage().isPresent()) {
            final var builder = toBuilder();
            state.page().ifPresent(builder::page);
            state.perPage().ifPresent(builder::perPage);
            return builder.build();
        }

        return this;
    }

    @Override
    public SearchType withFilter(Filter filter) {
        return toBuilder().filter(filter).build();
    }

    @Override
    public Set<String> effectiveStreams() {
        return ImmutableSet.of(DEFAULT_EVENTS_STREAM_ID, DEFAULT_SYSTEM_EVENTS_STREAM_ID);
    }

    @AutoValue.Builder
    public abstract static class Builder implements SearchTypeBuilder {
        @JsonCreator
        public static Builder createDefault() {
            return builder()
                    .filters(Collections.emptyList())
                    .streams(Collections.emptySet());
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(String id);

        abstract String id();

        @JsonProperty
        public abstract Builder name(@Nullable String name);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty(FIELD_SEARCH_FILTERS)
        public abstract Builder filters(List<UsedSearchFilter> filters);

        @JsonProperty
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty
        public abstract Builder timerange(@Nullable DerivedTimeRange timeRange);

        @JsonProperty
        public abstract Builder streams(Set<String> streams);

        @JsonProperty
        public abstract Builder page(@Nullable Integer page);

        abstract Optional<Integer> page();

        @JsonProperty
        public abstract Builder perPage(@Nullable Integer pageSize);

        abstract Optional<Integer> perPage();

        @JsonProperty
        public abstract Builder attributes(List<AttributeFilter> attributeFilters);

        abstract List<AttributeFilter> attributes();

        @JsonProperty
        public abstract Builder sort(@Nullable SortConfig sort);

        abstract EventList autoBuild();

        public EventList build() {
            if(id() == null) {
                id(UUID.randomUUID().toString());
            }

            checkArgument(page().orElse(1) > 0, "Page needs to be a positive, non-zero value");
            checkArgument(perPage().orElse(1) > 0, "Per page needs to be a positive, non-zero value");
            return autoBuild();
        }
    }

    @AutoValue
    @JsonTypeName(EventList.NAME)
    @JsonDeserialize(builder = EventList.Result.Builder.class)
    public abstract static class Result implements SearchType.Result {
        @Override
        @JsonProperty
        public abstract String id();

        @Override
        @JsonProperty
        public abstract String type();

        @JsonProperty
        public abstract List<CommonEventSummary> events();

        @JsonProperty
        public abstract long totalResults();

        public static Builder builder() {
            return new AutoValue_EventList_Result.Builder().type(EventList.NAME);
        }

        abstract Builder toBuilder();

        public Result withEvents(List<CommonEventSummary> events, long totalResults) {
            return toBuilder().events(events).totalResults(totalResults).build();
        }

        public static Builder result(String searchTypeId) {
            return builder().id(searchTypeId);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_EventList_Result.Builder().type(EventList.NAME);
            }

            @JsonProperty
            public abstract Builder id(String id);

            @JsonProperty
            public abstract Builder name(String name);

            @JsonProperty
            public abstract Builder type(String type);

            @JsonProperty
            public abstract Builder events(List<CommonEventSummary> events);

            @JsonProperty
            public abstract Builder totalResults(long count);

            public abstract Result build();
        }
    }

    @Override
    public SearchTypeEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return EventListEntity.builder()
                .streams(mappedStreams(entityDescriptorIds))
                .filter(filter())
                .filters(filters())
                .id(id())
                .name(name().orElse(null))
                .query(query().orElse(null))
                .type(type())
                .timerange(timerange().orElse(null))
                .build();
    }
}
