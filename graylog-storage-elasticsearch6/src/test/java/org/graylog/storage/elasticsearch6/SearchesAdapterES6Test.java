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
package org.graylog.storage.elasticsearch6;

import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import org.graylog2.Configuration;
import org.graylog2.indexer.results.FieldStatsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchesAdapterES6Test {
    private SearchesAdapterES6 searchesAdapter;

    @BeforeEach
    void setUp() {
        this.searchesAdapter = new SearchesAdapterES6(mock(Configuration.class), mock(MultiSearch.class), mock(Scroll.class), new SortOrderMapper());
    }

    @Test
    public void worksForNullFieldsInAggregationResults() throws Exception {
        final ExtendedStatsAggregation extendedStatsAggregation = mock(ExtendedStatsAggregation.class);

        when(extendedStatsAggregation.getCount()).thenReturn(null);
        when(extendedStatsAggregation.getSum()).thenReturn(null);
        when(extendedStatsAggregation.getSumOfSquares()).thenReturn(null);
        when(extendedStatsAggregation.getAvg()).thenReturn(null);
        when(extendedStatsAggregation.getMin()).thenReturn(null);
        when(extendedStatsAggregation.getMax()).thenReturn(null);
        when(extendedStatsAggregation.getVariance()).thenReturn(null);
        when(extendedStatsAggregation.getStdDeviation()).thenReturn(null);

        final FieldStatsResult result = searchesAdapter.createFieldStatsResult(null,
                extendedStatsAggregation,
                null,
                Collections.emptyList(),
                null,
                null,
                0);

        assertThat(result).isNotNull();
        assertThat(result.sum()).isEqualTo(Double.NaN);
        assertThat(result.sumOfSquares()).isEqualTo(Double.NaN);
        assertThat(result.mean()).isEqualTo(Double.NaN);
        assertThat(result.min()).isEqualTo(Double.NaN);
        assertThat(result.max()).isEqualTo(Double.NaN);
        assertThat(result.variance()).isEqualTo(Double.NaN);
        assertThat(result.stdDeviation()).isEqualTo(Double.NaN);

        assertThat(result.count()).isEqualTo(Long.MIN_VALUE);
        assertThat(result.cardinality()).isEqualTo(Long.MIN_VALUE);
    }
}
