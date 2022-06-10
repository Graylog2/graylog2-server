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
package org.graylog.plugins.views.search.searchfilters.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonTypeName(UsedSearchFilter.REFERENCED_SEARCH_FILTER)
@JsonDeserialize(builder = ReferencedQueryStringSearchFilter.Builder.class)
public abstract class ReferencedQueryStringSearchFilter implements ReferencedSearchFilter, QueryStringSearchFilter {

    @JsonProperty
    @Override
    public abstract String id();

    @JsonProperty(TITLE_FIELD)
    @Nullable
    public abstract String title();

    @JsonProperty(DESCRIPTION_FIELD)
    @Nullable
    public abstract String description();

    @JsonProperty(QUERY_STRING_FIELD)
    @Nullable
    public abstract String queryString();

    @Override
    @JsonProperty(value = NEGATION_FIELD, defaultValue = "false")
    public abstract boolean negation();

    @Override
    @JsonProperty(value = DISABLED_FIELD, defaultValue = "false")
    public abstract boolean disabled();

    public static ReferencedQueryStringSearchFilter create(final String id) {
        return builder().id(id).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public UsedSearchFilter withQueryString(String queryString) {
        return builder()
                .id(id())
                .title(title())
                .description(description())
                .queryString(queryString)
                .negation(negation())
                .disabled(disabled())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty(TITLE_FIELD)
        public abstract Builder title(String title);

        @JsonProperty(DESCRIPTION_FIELD)
        public abstract Builder description(String description);

        @JsonProperty(QUERY_STRING_FIELD)
        public abstract Builder queryString(String queryString);

        @JsonProperty(value = NEGATION_FIELD, defaultValue = "false")
        public abstract Builder negation(boolean negation);

        @JsonProperty(value = DISABLED_FIELD, defaultValue = "false")
        public abstract Builder disabled(boolean disabled);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ReferencedQueryStringSearchFilter.Builder()
                    .disabled(false)
                    .negation(false);
        }

        public abstract ReferencedQueryStringSearchFilter build();
    }
}
