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
@JsonTypeName(UsedSearchFilter.INLINE_QUERY_STRING_SEARCH_FILTER)
public abstract class InlineQueryStringSearchFilter implements UsedSearchFilter {

    @JsonProperty(TITLE_FIELD)
    @Nullable
    public abstract String title();

    @JsonProperty(DESCRIPTION_FIELD)
    @Nullable
    public abstract String description();

    @JsonProperty(QUERY_STRING_FIELD)
    public abstract String queryString();

    @JsonCreator
    @SuppressWarnings("unused")
    public static InlineQueryStringSearchFilter create(@JsonProperty(TITLE_FIELD) final String title,
                                                       @JsonProperty(DESCRIPTION_FIELD) final String description,
                                                       @JsonProperty(QUERY_STRING_FIELD) final String queryString) {
        return new AutoValue_InlineQueryStringSearchFilter(title, description, queryString);
    }
}
