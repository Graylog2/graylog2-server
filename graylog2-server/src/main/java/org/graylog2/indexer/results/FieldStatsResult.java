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

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class FieldStatsResult {
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
        return new AutoValue_FieldStatsResult(count, sum, sumOfSquares, mean, min, max, variance, stdDeviation, cardinality,
                hits, originalQuery, builtQuery, tookMs);
    }

    public static FieldStatsResult empty(String query, String bytesReference) {
        return create(Long.MIN_VALUE, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                Long.MIN_VALUE, Collections.emptyList(), query, bytesReference, 0);
    }

    public abstract long getCount();

    public abstract double getSum();

    public abstract double getSumOfSquares();

    public abstract double getMean();

    public abstract double getMin();

    public abstract double getMax();

    public abstract double getVariance();

    public abstract double getStdDeviation();

    public abstract long getCardinality();

    public abstract List<ResultMessage> getSearchHits();

    @Nullable
    public abstract String getOriginalQuery();

    @Nullable
    public abstract String getBuiltQuery();

    public abstract long getTookMs();
}
