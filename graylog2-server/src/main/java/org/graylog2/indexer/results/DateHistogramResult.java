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

import com.google.common.collect.Maps;
import io.searchbox.core.search.aggregation.HistogramAggregation;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.graylog2.indexer.searches.Searches;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Map;

public class DateHistogramResult extends HistogramResult {
    private final Map<Long, Long> result;
    private final Searches.DateHistogramInterval interval;

    public static DateHistogramResult empty(String originalQuery, BytesReference builtQuery, Searches.DateHistogramInterval interval, TimeValue took) {
        return new DateHistogramResult(originalQuery, builtQuery, interval, took);
    }

    private DateHistogramResult(String originalQuery, BytesReference builtQuery, Searches.DateHistogramInterval interval, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.result = Collections.emptyMap();
        this.interval = interval;
    }

    public DateHistogramResult(HistogramAggregation result, String originalQuery, BytesReference builtQuery, Searches.DateHistogramInterval interval, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.result = Maps.newTreeMap();

        for (HistogramAggregation.Histogram histogram : result.getBuckets()) {
            final DateTime keyAsDate = new DateTime(histogram.getKey());
            this.result.put(keyAsDate.getMillis() / 1000L, histogram.getCount());
        }

        this.interval = interval;
    }

    @Override
    public Searches.DateHistogramInterval getInterval() {
        return interval;
    }

    @Override
    public Map<Long, Long> getResults() {
        return result;
    }
}
