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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Query;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.graylog2.shared.utilities.ExceptionUtils.getRootCauseMessage;

public class QueryError implements SearchError {

    private final Query query;

    @Nullable
    private final Throwable throwable;

    private final String description;

    public QueryError(@Nonnull Query query, Throwable throwable) {
        this.query = query;
        this.throwable = throwable;
        this.description = getRootCauseMessage(throwable);
    }

    public QueryError(@Nonnull Query query, String description) {
        this.query = query;
        this.description = description;
        this.throwable = null;
    }

    @JsonProperty("query_id")
    public String queryId() {
        return query.id();
    }

    @Override
    public String description() {
        return description;
    }

    @Nullable
    @JsonProperty("backtrace")
    public String backtrace() {
        if (throwable == null) {
            return null;
        }
        return ExceptionUtils.getFullStackTrace(throwable);
    }
}
