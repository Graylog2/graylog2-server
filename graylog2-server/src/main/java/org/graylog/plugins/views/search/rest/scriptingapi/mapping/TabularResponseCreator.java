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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.apache.commons.collections.CollectionUtils;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.errors.SearchError;

import java.util.stream.Collectors;

public interface TabularResponseCreator {

    default void throwErrorIfAnyAvailable(final QueryResult queryResult) throws AggregationFailedException {
        if (!CollectionUtils.isEmpty(queryResult.errors())) {
            final String errorText = queryResult.errors().stream().map(SearchError::description).collect(Collectors.joining(", "));
            throw new AggregationFailedException("Failed to obtain aggregation results. Reason:" + errorText);
        }
    }


}
