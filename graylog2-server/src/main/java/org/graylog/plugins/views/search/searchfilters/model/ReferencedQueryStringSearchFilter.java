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

import static org.graylog.plugins.views.search.searchfilters.model.ModelConstants.Fields.DESCRIPTION;
import static org.graylog.plugins.views.search.searchfilters.model.ModelConstants.Fields.QUERY_STRING;
import static org.graylog.plugins.views.search.searchfilters.model.ModelConstants.Fields.TITLE;

@AutoValue
@JsonTypeName(UsedSearchFilter.REFERENCED_SEARCH_FILTER)
public abstract class ReferencedQueryStringSearchFilter implements ReferencedSearchFilter {

    @JsonProperty
    @Override
    public abstract String id();

    @JsonProperty(TITLE)
    @Nullable
    public abstract String title();

    @JsonProperty(DESCRIPTION)
    @Nullable
    public abstract String description();

    @JsonProperty(QUERY_STRING)
    @Nullable
    public abstract String queryString();

    @JsonCreator
    @SuppressWarnings("unused")
    public static ReferencedQueryStringSearchFilter create(@JsonProperty("id") final String id,
                                                           @JsonProperty(TITLE) final String title,
                                                           @JsonProperty(DESCRIPTION) final String description,
                                                           @JsonProperty(QUERY_STRING) final String queryString) {
        return new AutoValue_ReferencedQueryStringSearchFilter(id, title, description, queryString);
    }

    public static ReferencedQueryStringSearchFilter create(final String id) {
        return new AutoValue_ReferencedQueryStringSearchFilter(id, null, null, null);
    }
}
