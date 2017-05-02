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

import io.searchbox.core.search.aggregation.CardinalityAggregation;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import io.searchbox.core.search.aggregation.ValueCountAggregation;

import java.util.List;

public class FieldStatsResult extends IndexQueryResult {
    private List<ResultMessage> searchHits;

    private final long count;
    private final double sum;
    private final double sumOfSquares;
    private final double mean;
    private final double min;
    private final double max;
    private final double variance;
    private final double stdDeviation;
    private final long cardinality;

    public FieldStatsResult(ValueCountAggregation valueCountAggregation,
                            ExtendedStatsAggregation extendedStatsAggregation,
                            CardinalityAggregation cardinalityAggregation,
                            List<ResultMessage> hits,
                            String query,
                            String source,
                            long tookMs) {
        super(query, source, tookMs);
        this.count = getValueCount(valueCountAggregation, extendedStatsAggregation);
        this.cardinality = cardinalityAggregation == null ? Long.MIN_VALUE : cardinalityAggregation.getCardinality();

        if (extendedStatsAggregation != null) {
            sum = extendedStatsAggregation.getSum();
            sumOfSquares = extendedStatsAggregation.getSumOfSquares();
            mean = extendedStatsAggregation.getAvg();
            min = extendedStatsAggregation.getMin();
            max = extendedStatsAggregation.getMax();
            variance = extendedStatsAggregation.getVariance();
            stdDeviation = extendedStatsAggregation.getStdDeviation();
        } else {
            sum = Double.NaN;
            sumOfSquares = Double.NaN;
            mean = Double.NaN;
            min = Double.NaN;
            max = Double.NaN;
            variance = Double.NaN;
            stdDeviation = Double.NaN;
        }

        this.searchHits = hits;
    }

    private FieldStatsResult(String query, String bytesReference) {
        super(query, bytesReference, 0);

        this.count = Long.MIN_VALUE;
        this.cardinality = Long.MIN_VALUE;
        sum = Double.NaN;
        sumOfSquares = Double.NaN;
        mean = Double.NaN;
        min = Double.NaN;
        max = Double.NaN;
        variance = Double.NaN;
        stdDeviation = Double.NaN;
    }

    private long getValueCount(ValueCountAggregation valueCountAggregation, ExtendedStatsAggregation extendedStatsAggregation) {
        if (valueCountAggregation != null) {
            return valueCountAggregation.getValueCount();
        } else if (extendedStatsAggregation != null) {
            return extendedStatsAggregation.getCount();
        }
        return Long.MIN_VALUE;
    }

    public long getCount() {
        return count;
    }

    public double getSum() {
        return sum;
    }

    public double getSumOfSquares() {
        return sumOfSquares;
    }

    public double getMean() {
        return mean;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getVariance() {
        return variance;
    }

    public double getStdDeviation() {
        return stdDeviation;
    }

    public long getCardinality() {
        return cardinality;
    }

    public List<ResultMessage> getSearchHits() {
        return searchHits;
    }

    public static FieldStatsResult empty(String query, String bytesReference) {
        return new FieldStatsResult(query, bytesReference);
    }
}
