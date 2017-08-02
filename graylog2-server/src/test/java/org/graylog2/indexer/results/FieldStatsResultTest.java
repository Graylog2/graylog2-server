/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.results;

import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import org.junit.Test;

import java.util.Collections;

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