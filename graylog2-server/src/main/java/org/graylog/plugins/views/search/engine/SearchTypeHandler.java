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
package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.errors.SearchTypeError;

/**
 *
 * @param <S> the SearchType implementation this handler deals with
 * @param <Q> the backend-specific query builder
 * @param <R> the backend-specific result type
 */
@SuppressWarnings("unchecked")
public interface SearchTypeHandler<S extends SearchType, Q, R> {

    default void generateQueryPart(SearchJob job, Query query, SearchType searchType, Q queryContext) {
        // We need to typecast manually here, because '? extends SearchType' and 'SearchType' are never compatible
        // and thus the compiler won't accept the types at their call sites
        // This allows us to get proper types in the implementing classes instead of having to cast there.
        try {
            doGenerateQueryPart(job, query, (S) searchType, queryContext);
        } catch (SearchException e) {
            // these already have a specific error, so we don't need to handle them specially
            throw e;
        } catch (Throwable t) {
            throw new SearchException(new SearchTypeError(query, searchType.id(), t));
        }
    }

    void doGenerateQueryPart(SearchJob job, Query query, S searchType, Q queryContext);

    default SearchType.Result extractResult(SearchJob job, Query query, SearchType searchType, R queryResult, Q queryContext) {
        // see above for the reason for typecasting
        return doExtractResultImpl(job, query, (S) searchType, queryResult, queryContext);
    }

    SearchType.Result doExtractResultImpl(SearchJob job, Query query, S searchType, R queryResult, Q queryContext);
}
