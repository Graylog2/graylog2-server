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
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.rest.SearchTypeExecutionState;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.EventListEntity;
import org.graylog2.contentpacks.model.entities.SearchTypeEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

@AutoValue
@JsonTypeName(EventList.NAME)
@JsonDeserialize(builder = EventList.Builder.class)
public abstract class EventList implements SearchType {
    public static final String NAME = "events";

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

    @JsonCreator
    public static Builder builder() {
        return new AutoValue_EventList.Builder()
                .type(NAME)
                .filters(Collections.emptyList())
                .streams(Collections.emptySet());
    }

    public abstract Builder toBuilder();

    @Override
    public SearchType applyExecutionContext(SearchTypeExecutionState state) {
        return this;
    }

    @Override
    public SearchType withQuery(BackendQuery query) {
        return toBuilder().query(query).build();
    }

    @Override
    public SearchType withFilter(Filter filter) {
        return toBuilder().filter(filter).build();
    }

    @Override
    public SearchType withFilters(List<UsedSearchFilter> filters) {
        return toBuilder().filters(filters).build();
    }

    @Override
    public Set<String> effectiveStreams() {
        return ImmutableSet.of(DEFAULT_EVENTS_STREAM_ID, DEFAULT_SYSTEM_EVENTS_STREAM_ID);
    }

    @AutoValue.Builder
    public abstract static class Builder {
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

        abstract EventList autoBuild();

        public EventList build() {
            if(id() == null) {
                id(UUID.randomUUID().toString());
            }
            return autoBuild();
        }
    }

    @AutoValue
    public abstract static class Result implements SearchType.Result {
        @Override
        @JsonProperty
        public abstract String id();

        @Override
        @JsonProperty
        public String type() {
            return NAME;
        }

        @JsonProperty
        public abstract List<EventSummary> events();

        public static Builder builder() {
            return new AutoValue_EventList_Result.Builder();
        }

        public static Builder result(String searchTypeId) {
            return builder().id(searchTypeId);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(String id);

            public abstract Builder name(String name);

            public abstract Builder events(List<EventSummary> events);

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
