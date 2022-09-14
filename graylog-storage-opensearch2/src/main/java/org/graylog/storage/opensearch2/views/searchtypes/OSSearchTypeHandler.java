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
package org.graylog.storage.opensearch2.views.searchtypes;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.SearchTypeHandler;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;

/**
 * Signature of search type handlers the OpenSearch backend takes.
 * All of these take a {@link OSGeneratedQueryContext} as input.
 *
 * @param <S> the {@link SearchType SearchType} this handler deals with
 */
public interface OSSearchTypeHandler<S extends SearchType> extends SearchTypeHandler<S, OSGeneratedQueryContext, SearchResponse> {
    @Override
    default SearchType.Result doExtractResultImpl(SearchJob job, Query query, S searchType, SearchResponse queryResult, OSGeneratedQueryContext queryContext) {
        // if the search type was filtered, extract the sub-aggregation before passing it to the handler
        // this way we don't have to duplicate this step everywhere
        final Aggregations aggregations = queryResult.getAggregations();
        return doExtractResult(job, query, searchType, queryResult, aggregations, queryContext);
    }

    SearchType.Result doExtractResult(SearchJob job, Query query, S searchType, SearchResponse queryResult, Aggregations aggregations, OSGeneratedQueryContext queryContext);
}
