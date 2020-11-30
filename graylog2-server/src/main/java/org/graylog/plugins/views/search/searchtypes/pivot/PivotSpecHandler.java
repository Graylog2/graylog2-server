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
package org.graylog.plugins.views.search.searchtypes.pivot;

import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.SearchTypeHandler;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Implementations of this class contribute handlers for buckets and series to concrete implementations of {@link Pivot the pivot search type}.
 * @param <SPEC_TYPE> the type of bucket or series spec this handler deals with
 * @param <AGGREGATION_BUILDER> implementation specific type for building up the aggregation when generating a backend query
 * @param <QUERY_RESULT> the backend specific type holding the overall result from the backend
 * @param <AGGREGATION_RESULT> the backend specific type holding the partial result for the generated aggregation
 * @param <SEARCHTYPE_HANDLER> the backend specific type of the surrounding pivot search type handler
 * @param <QUERY_CONTEXT> an opaque context object to pass around information between query generation and result handling
 */
public interface PivotSpecHandler<SPEC_TYPE extends PivotSpec, AGGREGATION_BUILDER, QUERY_RESULT, AGGREGATION_RESULT, SEARCHTYPE_HANDLER, QUERY_CONTEXT> {

    @SuppressWarnings("unchecked")
    @Nonnull
    default Optional<AGGREGATION_BUILDER> createAggregation(String name, PivotSpec pivotSpec, SearchTypeHandler searchTypeHandler, GeneratedQueryContext queryContext) {
        return doCreateAggregation(name, (SPEC_TYPE) pivotSpec, (SEARCHTYPE_HANDLER) searchTypeHandler, (QUERY_CONTEXT) queryContext);
    }

    @Nonnull
    Optional<AGGREGATION_BUILDER> doCreateAggregation(String name, SPEC_TYPE pivotSpec, SEARCHTYPE_HANDLER searchTypeHandler, QUERY_CONTEXT queryContext);

    @SuppressWarnings("unchecked")
    default Object handleResult(PivotSpec pivotSpec, Object queryResult, Object aggregationResult, SearchTypeHandler searchTypeHandler, GeneratedQueryContext queryContext) {
        return doHandleResult((SPEC_TYPE) pivotSpec, (QUERY_RESULT) queryResult, (AGGREGATION_RESULT) aggregationResult, (SEARCHTYPE_HANDLER) searchTypeHandler, (QUERY_CONTEXT) queryContext);
    }

    Object doHandleResult(SPEC_TYPE pivotSpec, QUERY_RESULT queryResult, AGGREGATION_RESULT result, SEARCHTYPE_HANDLER searchTypeHandler, QUERY_CONTEXT queryContext);

}
