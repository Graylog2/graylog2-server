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
package org.graylog.plugins.views.search.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.search.Query;

import javax.annotation.Nonnull;

public class SearchTypeError extends QueryError {
    @Nonnull
    private final String searchTypeId;

    private final boolean fatal;

    public SearchTypeError(@Nonnull Query query, @Nonnull String searchTypeId, Throwable throwable) {
        super(query, throwable);

        this.searchTypeId = searchTypeId;
        this.fatal = false;
    }

    public SearchTypeError(@Nonnull Query query, @Nonnull String searchTypeId, String description) {
        this(query, searchTypeId, description, false);
    }

    public SearchTypeError(@Nonnull Query query, @Nonnull String searchTypeId, String description, boolean fatal) {
        super(query, description);

        this.searchTypeId = searchTypeId;
        this.fatal = fatal;
    }

    @Nonnull
    @JsonProperty("search_type_id")
    public String searchTypeId() {
        return searchTypeId;
    }

    @Override
    public boolean fatal() {
        return fatal;
    }
}
