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
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.buckets.OSValuesHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.SortOrder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OSPivotBucketSpecHandlerTest {
    private OSValuesHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OSValuesHandler();
    }

    private OSPivotBucketSpecHandler.SortOrders orderListForSortOnPivotField(String fieldType) {
        final PivotSort pivotSort = PivotSort.create("somefield", SortSpec.Direction.Ascending);
        final Pivot pivot = mock(Pivot.class);
        when(pivot.sort()).thenReturn(List.of(pivotSort));
        final Query query = mock(Query.class);
        when(query.effectiveStreams(pivot)).thenReturn(Set.of("stream1"));
        final OSGeneratedQueryContext queryContext = mock(OSGeneratedQueryContext.class);
        when(queryContext.fieldType(Set.of("stream1"), "somefield")).thenReturn(Optional.ofNullable(fieldType));

        return handler.orderListForPivot(pivot, queryContext, OSValuesHandler.DEFAULT_ORDER, query);
    }

    @Test
    void sortingOnNumericPivotFieldUsesSortHelperAggregation() {
        for (final String fieldType : List.of("long", "double", "float", "integer", "short", "byte", "half_float", "scaled_float")) {
            final OSPivotBucketSpecHandler.SortOrders sortOrders = orderListForSortOnPivotField(fieldType);

            assertThat(sortOrders.orders())
                    .as("sorting on pivot field of type <%s> should use the sort helper aggregation", fieldType)
                    .containsExactly(OSPivotBucketSpecHandler.BucketOrder.aggregation("sort_helpersomefield", SortOrder.Asc));
            assertThat(sortOrders.sortingAggregations())
                    .as("sorting on pivot field of type <%s> should create a sort helper aggregation", fieldType)
                    .hasEntrySatisfying("sort_helpersomefield", aggregation -> {
                        assertThat(aggregation.isMax()).isTrue();
                        assertThat(aggregation.max().field()).isEqualTo("somefield");
                    });
        }
    }

    @Test
    void sortingOnNonNumericPivotFieldUsesKeyOrder() {
        for (final String fieldType : List.of("keyword", "text", "date", "ip")) {
            final OSPivotBucketSpecHandler.SortOrders sortOrders = orderListForSortOnPivotField(fieldType);

            assertThat(sortOrders.orders())
                    .as("sorting on pivot field of type <%s> should use key order", fieldType)
                    .containsExactly(OSPivotBucketSpecHandler.BucketOrder.key(SortOrder.Asc));
            assertThat(sortOrders.sortingAggregations()).isEmpty();
        }
    }

    @Test
    void sortingOnPivotFieldWithUnknownTypeUsesKeyOrder() {
        final OSPivotBucketSpecHandler.SortOrders sortOrders = orderListForSortOnPivotField(null);

        assertThat(sortOrders.orders()).containsExactly(OSPivotBucketSpecHandler.BucketOrder.key(SortOrder.Asc));
        assertThat(sortOrders.sortingAggregations()).isEmpty();
    }

    private Pivot pivotWithSeries(boolean otherBucket, SeriesSpec... series) {
        return Pivot.builder()
                .rollup(true)
                .series(series)
                .rowGroups(Values.builder().field("source").limit(5).otherBucket(otherBucket).build())
                .build();
    }

    private List<MutableNamedAggregationBuilder> createMetrics(Pivot pivot) {
        final Query query = mock(Query.class);
        when(query.effectiveStreams(pivot)).thenReturn(Set.of("stream1"));
        final OSGeneratedQueryContext queryContext = mock(OSGeneratedQueryContext.class);
        final Values bucketSpec = (Values) pivot.rowGroups().get(0);

        return handler.doCreateAggregation(BucketSpecHandler.Direction.Row, "agg", pivot, bucketSpec, queryContext, query)
                .metrics();
    }

    private static List<String> statsAggregationNames(MutableNamedAggregationBuilder metric) {
        return metric.build().aggregations().keySet().stream()
                .filter(name -> name.startsWith("other-stats("))
                .sorted()
                .toList();
    }

    @Test
    void noStatsCompanionsWhenOtherBucketIsDisabled() {
        final Pivot pivot = pivotWithSeries(false, Average.builder().field("age").build());

        assertThat(createMetrics(pivot))
                .allSatisfy(metric -> assertThat(statsAggregationNames(metric)).isEmpty());
    }

    @Test
    void noStatsCompanionsWhenNoSeriesNeedsThem() {
        final Pivot pivot = pivotWithSeries(true,
                Count.builder().build(),
                Sum.builder().field("age").build());

        assertThat(createMetrics(pivot))
                .allSatisfy(metric -> assertThat(statsAggregationNames(metric)).isEmpty());
    }

    @Test
    void statsCompanionIsAddedToEveryMetricLevelForAverage() {
        final Pivot pivot = pivotWithSeries(true, Average.builder().field("age").build());

        assertThat(createMetrics(pivot))
                .isNotEmpty()
                .allSatisfy(metric -> assertThat(statsAggregationNames(metric))
                        .containsExactly("other-stats(age)"));
    }

    @Test
    void statsCompanionsAreDeduplicatedPerField() {
        final Pivot pivot = pivotWithSeries(true,
                Average.builder().field("age").build(),
                StdDev.builder().field("age").build(),
                Variance.builder().field("height").build());

        assertThat(createMetrics(pivot))
                .isNotEmpty()
                .allSatisfy(metric -> assertThat(statsAggregationNames(metric))
                        .containsExactly("other-stats(age)", "other-stats(height)"));
    }
}
