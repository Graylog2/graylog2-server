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
package org.graylog.events.procedures;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Set;

/**
 * Redirects the frontend to either a saved search or a defined search via the URL field.
 */
public class PerformSearch extends Action {
    public static final String NAME = "perform_search";
    public static final String FIELD_SAVED_SEARCH = "saved_search";
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_STREAMS = "streams";
    public static final String FIELD_STREAM_CATEGORIES = "stream_categories";

    @Inject
    public PerformSearch(@Assisted ActionDto dto) {
        super(dto);
    }

    public interface Factory extends Action.Factory<PerformSearch> {
        @Override
        PerformSearch create(ActionDto dto);
    }

    @AutoValue
    @JsonAutoDetect
    @JsonTypeName(NAME)
    @JsonDeserialize(builder = AutoValue_PerformSearch_Config.Builder.class)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public static abstract class Config implements ActionConfig {
        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @Nullable
        @JsonProperty(FIELD_SAVED_SEARCH)
        public abstract String savedSearch();

        @Nullable
        @JsonProperty(FIELD_QUERY)
        public abstract String query();

        @Nullable
        @JsonProperty(FIELD_STREAMS)
        public abstract Set<String> streams();

        @Nullable
        @JsonProperty(FIELD_STREAM_CATEGORIES)
        public abstract Set<String> streamCategories();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty(FIELD_SAVED_SEARCH)
            public abstract Builder savedSearch(String savedSearch);

            @JsonProperty(FIELD_QUERY)
            public abstract Builder query(String query);

            @JsonProperty(FIELD_STREAMS)
            public abstract Builder streams(Set<String> streams);

            @JsonProperty(FIELD_STREAM_CATEGORIES)
            public abstract Builder streamCategories(Set<String> streamCategories);

            @JsonCreator
            public static Builder create() {
                return new AutoValue_PerformSearch_Config.Builder().type(NAME);
            }

            public abstract Config build();
        }

    }
}
