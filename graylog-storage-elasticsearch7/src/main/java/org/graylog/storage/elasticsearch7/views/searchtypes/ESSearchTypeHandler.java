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
package org.graylog.storage.elasticsearch7.views.searchtypes;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.SearchTypeHandler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;

/**
 * Signature of search type handlers the elasticsearch backend takes.
 * All of these take a {@link ESGeneratedQueryContext} as input.
 *
 * @param <S> the {@link SearchType SearchType} this handler deals with
 */
public interface ESSearchTypeHandler<S extends SearchType> extends SearchTypeHandler<S, ESGeneratedQueryContext, SearchResponse> {
    @Override
    default SearchType.Result doExtractResultImpl(Query query, S searchType, SearchResponse queryResult, ESGeneratedQueryContext queryContext) {
        return doExtractResult(query, searchType, queryResult, queryContext);
    }

    SearchType.Result doExtractResult(Query query, S searchType, SearchResponse queryResult, ESGeneratedQueryContext queryContext);
}
