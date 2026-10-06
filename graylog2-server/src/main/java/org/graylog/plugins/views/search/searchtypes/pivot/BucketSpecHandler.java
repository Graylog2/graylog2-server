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
import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Implementations of this class contribute handlers for buckets concrete implementations of {@link Pivot the pivot search type}.
 *
 * @param <SPEC_TYPE>           the type of bucket spec this handler deals with
 * @param <AGGREGATION_BUILDER> implementation specific type for building up the aggregation when generating a backend query
 * @param <QUERY_CONTEXT>       an opaque context object to pass around information between query generation and result handling
 */
public interface BucketSpecHandler<SPEC_TYPE extends BucketSpec, AGGREGATION_BUILDER, QUERY_CONTEXT> {
    public enum Direction {
        Row,
        Column
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    default CreatedAggregations<AGGREGATION_BUILDER> createAggregation(Direction direction,
                                                                       String name,
                                                                       Pivot pivot,
                                                                       BucketSpec pivotSpec,
                                                                       GeneratedQueryContext queryContext,
                                                                       Query query) {
        return doCreateAggregation(direction, name, pivot, (SPEC_TYPE) pivotSpec, (QUERY_CONTEXT) queryContext, query);
    }

    @Nonnull
    CreatedAggregations<AGGREGATION_BUILDER> doCreateAggregation(Direction direction, String name, Pivot pivot, SPEC_TYPE bucketSpec, QUERY_CONTEXT queryContext, Query query);

    /**
     * A pivot field is sorted numerically only when its field type is a numeric one. Backend handlers use this to
     * decide whether to add a metric sub-aggregation as a sort helper instead of falling back to (lexicographic) key
     * order.
     */
    default boolean isSortOnNumericPivotField(Pivot pivot, PivotSort pivotSort, IndexerGeneratedQueryContext<?> queryContext, Query query) {
        return queryContext.fieldType(query.effectiveStreams(pivot), pivotSort.field())
                .filter(FieldTypeMapper::isNumericType)
                .isPresent();
    }

    record CreatedAggregations<T>(T root, T leaf, List<T> metrics) {
        public static <T> CreatedAggregations<T> create(T singleAggregation) {
            return new CreatedAggregations<>(singleAggregation, singleAggregation, List.of(singleAggregation));
        }

        public static <T> CreatedAggregations<T> create(T rootAggregation, T leafAggregation) {
            return new CreatedAggregations<>(rootAggregation, leafAggregation, List.of(leafAggregation));
        }

        public static <T> CreatedAggregations<T> create(T rootAggregation, T leafAggregation, List<T> metricsAggregations) {
            return new CreatedAggregations<>(rootAggregation, leafAggregation, metricsAggregations == null ? Collections.emptyList() : metricsAggregations);
        }
    }
}
