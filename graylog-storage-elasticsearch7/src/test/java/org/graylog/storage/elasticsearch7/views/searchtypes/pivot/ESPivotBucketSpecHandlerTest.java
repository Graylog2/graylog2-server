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
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.BucketOrder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets.ESValuesHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ESPivotBucketSpecHandlerTest {
    private ESValuesHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ESValuesHandler();
    }

    private ESPivotBucketSpecHandler.SortOrders orderListForSortOnPivotField(String fieldType) {
        final PivotSort pivotSort = PivotSort.create("somefield", SortSpec.Direction.Ascending);
        final Pivot pivot = mock(Pivot.class);
        when(pivot.sort()).thenReturn(List.of(pivotSort));
        final Query query = mock(Query.class);
        when(query.effectiveStreams(pivot)).thenReturn(Set.of("stream1"));
        final ESGeneratedQueryContext queryContext = mock(ESGeneratedQueryContext.class);
        when(queryContext.fieldType(Set.of("stream1"), "somefield")).thenReturn(Optional.ofNullable(fieldType));

        return handler.orderListForPivot(pivot, queryContext, ESValuesHandler.DEFAULT_ORDER, query);
    }

    @Test
    void sortingOnNumericPivotFieldUsesSortHelperAggregation() {
        for (final String fieldType : List.of("long", "double", "float", "integer", "short", "byte", "half_float", "scaled_float")) {
            final ESPivotBucketSpecHandler.SortOrders sortOrders = orderListForSortOnPivotField(fieldType);

            assertThat(sortOrders.orders())
                    .as("sorting on pivot field of type <%s> should use the sort helper aggregation", fieldType)
                    .containsExactly(BucketOrder.aggregation("sort_helpersomefield", true));
            assertThat(sortOrders.sortingAggregations())
                    .as("sorting on pivot field of type <%s> should create a sort helper aggregation", fieldType)
                    .satisfiesExactly(aggregation -> assertThat(aggregation)
                            .isInstanceOfSatisfying(MaxAggregationBuilder.class, maxAggregation -> {
                                assertThat(maxAggregation.getName()).isEqualTo("sort_helpersomefield");
                                assertThat(maxAggregation.field()).isEqualTo("somefield");
                            }));
        }
    }

    @Test
    void sortingOnNonNumericPivotFieldUsesKeyOrder() {
        for (final String fieldType : List.of("keyword", "text", "date", "ip")) {
            final ESPivotBucketSpecHandler.SortOrders sortOrders = orderListForSortOnPivotField(fieldType);

            assertThat(sortOrders.orders())
                    .as("sorting on pivot field of type <%s> should use key order", fieldType)
                    .containsExactly(BucketOrder.key(true));
            assertThat(sortOrders.sortingAggregations()).isEmpty();
        }
    }

    @Test
    void sortingOnPivotFieldWithUnknownTypeUsesKeyOrder() {
        final ESPivotBucketSpecHandler.SortOrders sortOrders = orderListForSortOnPivotField(null);

        assertThat(sortOrders.orders()).containsExactly(BucketOrder.key(true));
        assertThat(sortOrders.sortingAggregations()).isEmpty();
    }
}
