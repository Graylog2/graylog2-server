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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.graylog2.indexer.searches.Searches;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

import java.util.Collections;
import java.util.Map;

public class FieldHistogramResult extends HistogramResult {
    private static final Map<String, Number> EMPTY_RESULT = ImmutableMap.<String, Number>builder()
            .put("count", 0)
            .put("min", 0)
            .put("max", 0)
            .put("total", 0)
            .put("total_count", 0)
            .put("mean", 0)
            .build();

    private final Histogram result;
    private final Searches.DateHistogramInterval interval;

    public FieldHistogramResult(Histogram result, String originalQuery, BytesReference builtQuery, Searches.DateHistogramInterval interval, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.result = result;
        this.interval = interval;
    }

    public Searches.DateHistogramInterval getInterval() {
        return interval;
    }

    public Map<Long, Map<String, Number>> getResults() {
        if (result.getBuckets().isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Long, Map<String, Number>> results = Maps.newTreeMap();
        for (Histogram.Bucket b : result.getBuckets()) {
            final ImmutableMap.Builder<String, Number> resultMap = ImmutableMap.builder();
            resultMap.put("total_count", b.getDocCount());

            final Stats stats = b.getAggregations().get(Searches.AGG_STATS);
            resultMap.put("count", stats.getCount());
            resultMap.put("min", stats.getMin());
            resultMap.put("max", stats.getMax());
            resultMap.put("total", stats.getSum());
            resultMap.put("mean", stats.getAvg());

            // cardinality is only calculated if it was explicitly requested, so this might be null
            final Cardinality cardinality = b.getAggregations().get(Searches.AGG_CARDINALITY);
            resultMap.put("cardinality", cardinality == null ? 0 : cardinality.getValue());

            final DateTime keyAsDate = (DateTime) b.getKey();
            final long timestamp = keyAsDate.getMillis() / 1000L;
            results.put(timestamp, resultMap.build());
        }

        final long minTimestamp = Collections.min(results.keySet());
        final long maxTimestamp = Collections.max(results.keySet());
        final MutableDateTime currentTime = new MutableDateTime(minTimestamp, DateTimeZone.UTC);

        while (currentTime.getMillis() < maxTimestamp) {
            final Map<String, Number> entry = results.get(currentTime.getMillis());

            // advance timestamp by the interval's seconds value
            currentTime.add(interval.getPeriod());

            if (entry == null) {
                // synthesize a 0 value for this timestamp
                results.put(currentTime.getMillis(), EMPTY_RESULT);
            }
        }
        return results;
    }
}
