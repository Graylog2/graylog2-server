/*
 * Copyright (C) 2025 Graylog, Inc.
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
package org.graylog.events.processor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Describes the search inputs required to replay an event definition.
 */
@AutoValue
@JsonDeserialize(builder = EventDefinitionReplayInfo.Builder.class)
public abstract class EventDefinitionReplayInfo {
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_STREAMS = "streams";
    public static final String FIELD_STREAM_CATEGORIES = "stream_categories";
    public static final String FIELD_FILTERS = "filters";
    public static final String FIELD_SEARCH_WITHIN_MS = "search_within_ms";

    @Nullable
    @JsonProperty(FIELD_QUERY)
    public abstract String query();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_STREAM_CATEGORIES)
    public abstract Set<String> streamCategories();

    @JsonProperty(FIELD_FILTERS)
    public abstract List<UsedSearchFilter> filters();

    @JsonProperty(FIELD_SEARCH_WITHIN_MS)
    public abstract long searchWithinMs();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        private static Builder create() {
            return new AutoValue_EventDefinitionReplayInfo.Builder()
                    .streams(ImmutableSet.of())
                    .streamCategories(ImmutableSet.of())
                    .filters(ImmutableList.of())
                    .searchWithinMs(0L);
        }

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(String query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_STREAM_CATEGORIES)
        public abstract Builder streamCategories(Set<String> streamCategories);

        @JsonProperty(FIELD_FILTERS)
        public abstract Builder filters(List<UsedSearchFilter> filters);

        @JsonProperty(FIELD_SEARCH_WITHIN_MS)
        public abstract Builder searchWithinMs(long searchWithinMs);

        public abstract EventDefinitionReplayInfo build();
    }
}
