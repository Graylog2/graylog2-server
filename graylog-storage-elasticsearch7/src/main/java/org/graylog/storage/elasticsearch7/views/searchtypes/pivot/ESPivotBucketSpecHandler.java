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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.BucketOrder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ESPivotBucketSpecHandler<SPEC_TYPE extends BucketSpec>
        implements BucketSpecHandler<SPEC_TYPE, AggregationBuilder, ESGeneratedQueryContext> {

    protected AggTypes aggTypes(ESGeneratedQueryContext queryContext, Pivot pivot) {
        return (AggTypes) queryContext.contextMap().get(pivot.id());
    }

    protected void record(ESGeneratedQueryContext queryContext, Pivot pivot, PivotSpec spec, String name, Class<? extends Aggregation> aggregationClass) {
        aggTypes(queryContext, pivot).record(spec, name, aggregationClass);
    }

    public record SortOrders(List<BucketOrder> orders, List<AggregationBuilder> sortingAggregations) {}

    protected SortOrders orderListForPivot(Pivot pivot, ESGeneratedQueryContext esGeneratedQueryContext, BucketOrder defaultOrder, Query query) {
        final List<AggregationBuilder> sortingAggregations = new ArrayList<>();
        final List<BucketOrder> ordering = pivot.sort()
                .stream()
                .map(sortSpec -> {
                    final var isAscending = sortSpec.direction().equals(SortSpec.Direction.Ascending);
                    if (sortSpec instanceof PivotSort pivotSort) {
                        if (isSortOnNumericPivotField(pivot, pivotSort, esGeneratedQueryContext, query)) {
                            /* When we sort on a numeric pivot field, we create a metric sub-aggregation for that field, which returns
                            the numeric value of it, so that we can sort on it numerically. Any metric aggregation (min/max/avg) will work. */
                            final var aggregationName = "sort_helper" + pivotSort.field();
                            sortingAggregations.add(AggregationBuilders.max(aggregationName).field(pivotSort.field()));
                            return BucketOrder.aggregation(aggregationName, isAscending);
                        } else {
                            return BucketOrder.key(isAscending);
                        }
                    }
                    if (sortSpec instanceof SeriesSort) {
                        final Optional<SeriesSpec> matchingSeriesSpec = pivot.series()
                                .stream()
                                .filter(series -> series.literal().equals(sortSpec.field()))
                                .findFirst();
                        return matchingSeriesSpec
                                .map(seriesSpec -> {
                                    if (seriesSpec.literal().equals("count()")) {
                                        return BucketOrder.count(isAscending);
                                    }

                                    String orderPath = seriesSpec.statsSubfieldName()
                                            .map(subField -> esGeneratedQueryContext.seriesName(seriesSpec, pivot) + "." + subField)
                                            .orElse(esGeneratedQueryContext.seriesName(seriesSpec, pivot));

                                    return BucketOrder.aggregation(orderPath, isAscending);
                                })
                                .orElse(null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return ordering.isEmpty()
                ? new SortOrders(List.of(defaultOrder), List.of())
                : new SortOrders(ordering, List.copyOf(sortingAggregations));
    }

    private boolean isSortOnNumericPivotField(Pivot pivot, PivotSort pivotSort, ESGeneratedQueryContext queryContext, Query query) {
        return queryContext.fieldType(query.effectiveStreams(pivot), pivotSort.field())
                .filter(this::isNumericFieldType)
                .isPresent();
    }

    private boolean isNumericFieldType(String fieldType) {
        return fieldType.equals("long") || fieldType.equals("double") || fieldType.equals("float");
    }

    public abstract Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpec, PivotBucket initialBucket);
}
