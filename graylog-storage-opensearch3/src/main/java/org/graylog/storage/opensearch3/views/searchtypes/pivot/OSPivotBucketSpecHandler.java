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
package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class OSPivotBucketSpecHandler<SPEC_TYPE extends BucketSpec>
        implements BucketSpecHandler<SPEC_TYPE, MutableNamedAggregationBuilder, OSGeneratedQueryContext> {

    public record BucketOrder(Type type, String name, SortOrder order) {
        public enum Type {KEY, COUNT, AGGREGATION}

        public static BucketOrder key(SortOrder order) {
            return new BucketOrder(Type.KEY, null, order);
        }

        public static BucketOrder count(SortOrder order) {
            return new BucketOrder(Type.COUNT, null, order);
        }

        public static BucketOrder aggregation(String name, SortOrder order) {
            return new BucketOrder(Type.AGGREGATION, name, order);
        }
    }

    public record SortOrders(List<BucketOrder> orders, Map<String, Aggregation> sortingAggregations) {
    }

    protected SortOrders orderListForPivot(Pivot pivot, IndexerGeneratedQueryContext<?> queryContext, BucketOrder defaultOrder, Query query) {
        final Map<String, Aggregation> sortingAggregations = new LinkedHashMap<>();
        var ordering = pivot.sort()
                .stream()
                .map(sortSpec -> {
                    final var isAscending = sortSpec.direction().equals(SortSpec.Direction.Ascending);
                    final var order = isAscending ? SortOrder.Asc : SortOrder.Desc;

                    if (sortSpec instanceof PivotSort pivotSort) {
                        if (isSortOnNumericPivotField(pivot, pivotSort, queryContext, query)) {
                    /* When we sort on a numeric pivot field, we create a metric sub-aggregation for that field, which returns
                    the numeric value of it, so that we can sort on it numerically. Any metric aggregation (min/max/avg) will work. */
                            final var aggregationName = "sort_helper" + pivotSort.field();

                            // Create a Max aggregation as the sort helper
                            Aggregation maxAgg = new Aggregation.Builder()
                                    .max(m -> m.field(pivotSort.field()))
                                    .build();

                            sortingAggregations.put(aggregationName, maxAgg);
                            return BucketOrder.aggregation(aggregationName, order);
                        } else {
                            return BucketOrder.key(order);
                        }
                    }
                    if (sortSpec instanceof SeriesSort) {
                        return pivot.series().stream()
                                .filter(series -> series.literal().equals(sortSpec.field())
                                        || (series instanceof Percentile && sortSpec.field().equals(series.id())))
                                .findFirst()
                                .map(seriesSpec -> {
                                    if (seriesSpec.literal().equals("count()")) {
                                        return BucketOrder.count(order);
                                    } else {
                                        String orderPath = seriesSpec.multiValueAggSubfieldName()
                                                .map(subField -> queryContext.seriesName(seriesSpec, pivot) + "[" + subField + "]")
                                                .orElse(queryContext.seriesName(seriesSpec, pivot));

                                        return BucketOrder.aggregation(orderPath, order);
                                    }
                                })
                                .orElse(null);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return ordering.isEmpty()
                ? new SortOrders(List.of(defaultOrder), Map.of())
                : new SortOrders(ordering, Map.copyOf(sortingAggregations));
    }

    private boolean isSortOnNumericPivotField(Pivot pivot, PivotSort pivotSort, IndexerGeneratedQueryContext<?> queryContext, Query query) {
        return queryContext.fieldType(query.effectiveStreams(pivot), pivotSort.field())
                .filter(this::isNumericFieldType)
                .isPresent();
    }

    private boolean isNumericFieldType(String fieldType) {
        return fieldType.equals("long") || fieldType.equals("double") || fieldType.equals("float");
    }

    public abstract Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket previousBucket);
}
