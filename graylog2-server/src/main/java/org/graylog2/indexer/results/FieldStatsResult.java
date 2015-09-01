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

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStats;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCount;

import javax.annotation.Nullable;
import java.util.List;

public class FieldStatsResult extends IndexQueryResult {

    @Nullable
    private final ValueCount valueCount;
    @Nullable
    private final ExtendedStats extendedStats;
    @Nullable
    private final Cardinality cardinality;
    private List<ResultMessage> searchHits;


    public FieldStatsResult(@Nullable ValueCount valueCount,
                            @Nullable ExtendedStats extendedStats,
                            @Nullable Cardinality cardinality,
                            SearchHits hits,
                            String query,
                            BytesReference source, TimeValue took) {
        super(query, source, took);
        this.valueCount = valueCount;
        this.extendedStats = extendedStats;
        this.cardinality = cardinality;
        this.searchHits = buildResults(hits);
    }

    public long getCount() {
        if (valueCount != null) {
            return valueCount.getValue();
        } else if (extendedStats != null) {
            return extendedStats.getCount();
        }
        return Long.MIN_VALUE;
    }

    public double getSum() {
        return extendedStats != null ? extendedStats.getSum() : Double.NaN;
    }

    public double getSumOfSquares() {
        return extendedStats != null ? extendedStats.getSumOfSquares() : Double.NaN;
    }

    public double getMean() {
        return extendedStats != null ? extendedStats.getAvg() : Double.NaN;
    }

    public double getMin() {
        return extendedStats != null ? extendedStats.getMin() : Double.NaN;
    }

    public double getMax() {
        return extendedStats != null ? extendedStats.getMax() : Double.NaN;
    }

    public double getVariance() {
        return extendedStats != null ? extendedStats.getVariance() : Double.NaN;
    }

    public double getStdDeviation() {
        return extendedStats != null ? extendedStats.getStdDeviation() : Double.NaN;
    }


    public long getCardinality() {
        return cardinality != null ? cardinality.getValue() : Long.MIN_VALUE;
    }

    public List<ResultMessage> getSearchHits() {
        return searchHits;
    }
}
