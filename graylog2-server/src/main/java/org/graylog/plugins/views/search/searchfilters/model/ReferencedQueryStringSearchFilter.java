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
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonTypeName(UsedSearchFilter.REFERENCED_SEARCH_FILTER)
public abstract class ReferencedQueryStringSearchFilter implements ReferencedSearchFilter {

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
    @JsonProperty(NEGATION_FIELD)
    @Nullable
    public abstract Boolean negation();

    @JsonCreator
    @SuppressWarnings("unused")
    public static ReferencedQueryStringSearchFilter create(@JsonProperty("id") final String id,
                                                           @JsonProperty(TITLE_FIELD) final String title,
                                                           @JsonProperty(DESCRIPTION_FIELD) final String description,
                                                           @JsonProperty(QUERY_STRING_FIELD) final String queryString,
                                                           @JsonProperty(NEGATION_FIELD) final Boolean negation) {
        return new AutoValue_ReferencedQueryStringSearchFilter(id, title, description, queryString, negation);
    }

    public static ReferencedQueryStringSearchFilter create(@JsonProperty("id") final String id,
                                                           @JsonProperty(TITLE_FIELD) final String title,
                                                           @JsonProperty(DESCRIPTION_FIELD) final String description,
                                                           @JsonProperty(QUERY_STRING_FIELD) final String queryString) {
        return new AutoValue_ReferencedQueryStringSearchFilter(id, title, description, queryString, null);
    }

    public static ReferencedQueryStringSearchFilter create(final String id) {
        return new AutoValue_ReferencedQueryStringSearchFilter(id, null, null, null, null);
    }
}
