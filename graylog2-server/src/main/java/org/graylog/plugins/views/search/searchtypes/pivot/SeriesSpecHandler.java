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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementations of this class contribute handlers for series to concrete implementations of {@link Pivot the pivot search type}.
 * @param <SPEC_TYPE> the type of series spec this handler deals with
 * @param <AGGREGATION_BUILDER> implementation specific type for building up the aggregation when generating a backend query
 * @param <QUERY_RESULT> the backend specific type holding the overall result from the backend
 * @param <AGGREGATION_RESULT> the backend specific type holding the partial result for the generated aggregation
 * @param <QUERY_CONTEXT> an opaque context object to pass around information between query generation and result handling
 */
public interface SeriesSpecHandler<SPEC_TYPE extends SeriesSpec, AGGREGATION_BUILDER, QUERY_RESULT, AGGREGATION_RESULT, QUERY_CONTEXT> {

    @SuppressWarnings("unchecked")
    @Nonnull
    default List<AGGREGATION_BUILDER> createAggregation(String name, Pivot pivot, SeriesSpec seriesSpec, QUERY_CONTEXT queryContext) {
        return doCreateAggregation(name, pivot, (SPEC_TYPE) seriesSpec, queryContext);
    }

    @Nonnull
    List<AGGREGATION_BUILDER> doCreateAggregation(String name, Pivot pivot, SPEC_TYPE seriesSpec, QUERY_CONTEXT queryContext);

    default Stream<Value> handleResult(Pivot pivot, SPEC_TYPE seriesSpec, QUERY_RESULT queryResult, AGGREGATION_RESULT aggregationResult, QUERY_CONTEXT queryContext) {
        return doHandleResult(pivot, seriesSpec, queryResult, aggregationResult, queryContext);
    }

    Stream<Value> doHandleResult(Pivot pivot, SPEC_TYPE seriesSpec, QUERY_RESULT queryResult, AGGREGATION_RESULT result, QUERY_CONTEXT queryContext);


    record Value(String id, String key, Object value) {

        public static Value create(String id, String key, Object value) {
            return new Value(id, key, value);
        }
    }
}
