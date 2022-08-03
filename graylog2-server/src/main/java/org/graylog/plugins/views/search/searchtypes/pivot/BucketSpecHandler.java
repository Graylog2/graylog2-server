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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Implementations of this class contribute handlers for buckets concrete implementations of {@link Pivot the pivot search type}.
 *
 * @param <SPEC_TYPE>           the type of bucket spec this handler deals with
 * @param <AGGREGATION_BUILDER> implementation specific type for building up the aggregation when generating a backend query
 * @param <QUERY_RESULT>        the backend specific type holding the overall result from the backend
 * @param <AGGREGATION_RESULT>  the backend specific type holding the partial result for the generated aggregation
 * @param <QUERY_CONTEXT>       an opaque context object to pass around information between query generation and result handling
 */
public interface BucketSpecHandler<SPEC_TYPE extends BucketSpec, AGGREGATION_BUILDER, QUERY_RESULT, AGGREGATION_RESULT, QUERY_CONTEXT> {

    @SuppressWarnings("unchecked")
    @Nonnull
    default Optional<AGGREGATION_BUILDER> createAggregation(String name, Pivot pivot, PivotSpec pivotSpec, GeneratedQueryContext queryContext, Query query) {
        return doCreateAggregation(name, pivot, (SPEC_TYPE) pivotSpec, (QUERY_CONTEXT) queryContext, query);
    }

    @Nonnull
    Optional<AGGREGATION_BUILDER> doCreateAggregation(String name, Pivot pivot, SPEC_TYPE bucketSpec, QUERY_CONTEXT queryContext, Query query);

    @SuppressWarnings("unchecked")
    default Object handleResult(BucketSpec bucketSpec, Object aggregationResult) {
        return doHandleResult((SPEC_TYPE) bucketSpec, (AGGREGATION_RESULT) aggregationResult);
    }

    Object doHandleResult(SPEC_TYPE bucketSpec, AGGREGATION_RESULT result);

}
