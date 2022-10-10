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
import java.util.List;
import java.util.Optional;

/**
 * Implementations of this class contribute handlers for buckets concrete implementations of {@link Pivot the pivot search type}.
 *
 * @param <SPEC_TYPE>           the type of bucket spec this handler deals with
 * @param <AGGREGATION_BUILDER> implementation specific type for building up the aggregation when generating a backend query
 * @param <QUERY_CONTEXT>       an opaque context object to pass around information between query generation and result handling
 */
public interface BucketSpecHandler<SPEC_TYPE extends BucketSpec, AGGREGATION_BUILDER, QUERY_CONTEXT> {

    @SuppressWarnings("unchecked")
    @Nonnull
    default Optional<CreatedAggregations<AGGREGATION_BUILDER>> createAggregation(String name,
                                                                                         Pivot pivot,
                                                                                         List<BucketSpec> pivotSpec,
                                                                                         GeneratedQueryContext queryContext,
                                                                                         Query query) {
        return doCreateAggregation(name, pivot, (List<SPEC_TYPE>) pivotSpec, (QUERY_CONTEXT) queryContext, query);
    }

    @Nonnull
    Optional<CreatedAggregations<AGGREGATION_BUILDER>> doCreateAggregation(String name, Pivot pivot, List<SPEC_TYPE> bucketSpec, QUERY_CONTEXT queryContext, Query query);

    record CreatedAggregations<T>(T root, T leaf, List<T> metrics) {
        public static <T> CreatedAggregations<T> create(T singleAggregation) {
            return new CreatedAggregations<>(singleAggregation, singleAggregation, List.of(singleAggregation));
        }

        public static <T> CreatedAggregations<T> create(T rootAggregation, T leafAggregation) {
            return new CreatedAggregations<>(rootAggregation, leafAggregation, List.of(leafAggregation));
        }

        public static <T> CreatedAggregations<T> create(T rootAggregation, T leafAggregation, List<T> metricsAggregations) {
            return new CreatedAggregations<>(rootAggregation, leafAggregation, metricsAggregations);
        }
    }
}
