/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.indexer.results;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldStatsResult extends IndexQueryResult {

    private final long count;
    private final double sum;
    private final double sumOfSquares;
    private final double mean;
    private final double min;
    private final double max;
    private final double variance;
    private final double stdDeviation;
    private List<ResultMessage> searchHits;

    public FieldStatsResult(StatisticalFacet f, String originalQuery, BytesReference builtQuery, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.count = f.getCount();
        this.sum = f.getTotal();
        this.sumOfSquares = f.getSumOfSquares();
        this.mean = f.getMean();
        this.min = f.getMin();
        this.max = f.getMax();
        this.variance = f.getVariance();
        this.stdDeviation = f.getStdDeviation();
    }

    public FieldStatsResult(StatisticalFacet facet, SearchHits searchHits, String query, BytesReference source, TimeValue took) {
        this(facet, query, source, took);
        this.searchHits = buildResults(searchHits);
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

    public List<ResultMessage> getSearchHits() {
        return searchHits;
    }
}
