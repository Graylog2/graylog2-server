package org.graylog2.indexer.results;

import edu.emory.mathcs.backport.java.util.Collections;
import io.searchbox.core.search.aggregation.CardinalityAggregation;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import io.searchbox.core.search.aggregation.ValueCountAggregation;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldStatsResultTest {
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

        final FieldStatsResult result = new FieldStatsResult(null,
                extendedStatsAggregation,
                null,
                Collections.emptyList(),
                null,
                null,
                0);

        assertThat(result).isNotNull();
        assertThat(result.getSum()).isEqualTo(Double.NaN);
        assertThat(result.getSumOfSquares()).isEqualTo(Double.NaN);
        assertThat(result.getMean()).isEqualTo(Double.NaN);
        assertThat(result.getMin()).isEqualTo(Double.NaN);
        assertThat(result.getMax()).isEqualTo(Double.NaN);
        assertThat(result.getVariance()).isEqualTo(Double.NaN);
        assertThat(result.getStdDeviation()).isEqualTo(Double.NaN);

        assertThat(result.getCount()).isEqualTo(Long.MIN_VALUE);
        assertThat(result.getCardinality()).isEqualTo(Long.MIN_VALUE);
    }
}