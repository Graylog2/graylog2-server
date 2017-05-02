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
import io.searchbox.core.search.aggregation.CardinalityAggregation;
import io.searchbox.core.search.aggregation.HistogramAggregation;
import io.searchbox.core.search.aggregation.StatsAggregation;
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

    private final Map<Long, Map<String, Number>> result;
    private final Searches.DateHistogramInterval interval;

    public FieldHistogramResult(HistogramAggregation histogramAggregation, String originalQuery, String builtQuery, Searches.DateHistogramInterval interval, long tookMs) {
        super(originalQuery, builtQuery, tookMs);

        this.interval = interval;
        this.result = getResultsFromHistogramAggregation(histogramAggregation);
    }

    @Override
    public Searches.DateHistogramInterval getInterval() {
        return interval;
    }

    @Override
    public Map getResults() {
        return result;
    }

    private Map<Long, Map<String, Number>> getResultsFromHistogramAggregation(HistogramAggregation histogramAggregation) {
        if (histogramAggregation.getBuckets().isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Long, Map<String, Number>> results = Maps.newTreeMap();
        for (HistogramAggregation.Histogram b : histogramAggregation.getBuckets()) {
            final ImmutableMap.Builder<String, Number> resultMap = ImmutableMap.builder();
            resultMap.put("total_count", b.getCount());

            final StatsAggregation stats = b.getStatsAggregation(Searches.AGG_STATS);
            resultMap.put("count", stats.getCount());
            resultMap.put("min", stats.getMin() == null ? 0D : stats.getMin());
            resultMap.put("max", stats.getMax() == null ? 0D : stats.getMax());
            resultMap.put("total", stats.getSum() == null ? 0D : stats.getSum());
            resultMap.put("mean", stats.getAvg() == null ? 0D : stats.getAvg());

            // cardinality is only calculated if it was explicitly requested, so this might be null
            final CardinalityAggregation cardinality = b.getCardinalityAggregation(Searches.AGG_CARDINALITY);
            resultMap.put("cardinality", cardinality == null ? 0 : cardinality.getCardinality());

            final DateTime keyAsDate = new DateTime(b.getKey());
            final long timestamp = keyAsDate.getMillis() / 1000L;
            results.put(timestamp, resultMap.build());
        }

        fillEmptyTimestamps(results);
        return results;
    }

    private void fillEmptyTimestamps(Map<Long, Map<String, Number>> results) {
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
    }

    private FieldHistogramResult(String originalQuery, String builtQuery, Searches.DateHistogramInterval interval) {
        super(originalQuery, builtQuery, 0);

        this.result = Collections.emptyMap();
        this.interval = interval;
    }
    public static HistogramResult empty(String originalQuery, String builtQuery, Searches.DateHistogramInterval interval) {
        return new FieldHistogramResult(originalQuery, builtQuery, interval);
    }
}
