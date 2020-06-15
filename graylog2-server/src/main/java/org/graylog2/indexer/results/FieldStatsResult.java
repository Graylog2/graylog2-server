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

import java.util.Collections;
import java.util.List;

public class FieldStatsResult extends IndexQueryResult {
    private final List<ResultMessage> searchHits;

    private final long count;
    private final double sum;
    private final double sumOfSquares;
    private final double mean;
    private final double min;
    private final double max;
    private final double variance;
    private final double stdDeviation;
    private final long cardinality;

    public static FieldStatsResult create(long count,
                                          double sum,
                                          double sumOfSquares,
                                          double mean,
                                          double min,
                                          double max,
                                          double variance,
                                          double stdDeviation,
                                          long cardinality,
                                          List<ResultMessage> hits,
                                          String originalQuery,
                                          String builtQuery,
                                          long tookMs) {
        return new FieldStatsResult(originalQuery, builtQuery, tookMs, hits, count, sum, sumOfSquares, mean, min, max,
                variance, stdDeviation, cardinality);
    }

    public FieldStatsResult(String originalQuery, String builtQuery, long tookMs, List<ResultMessage> searchHits, long count, double sum, double sumOfSquares, double mean, double min, double max, double variance, double stdDeviation, long cardinality) {
        super(originalQuery, builtQuery, tookMs);
        this.searchHits = searchHits;
        this.count = count;
        this.sum = sum;
        this.sumOfSquares = sumOfSquares;
        this.mean = mean;
        this.min = min;
        this.max = max;
        this.variance = variance;
        this.stdDeviation = stdDeviation;
        this.cardinality = cardinality;
    }

    public static FieldStatsResult empty(String query, String bytesReference) {
        return create(Long.MIN_VALUE, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                Long.MIN_VALUE, Collections.emptyList(), query, bytesReference, 0);
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
}
