/**
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
 */
package org.graylog2.indexer.results;

import com.google.common.collect.Maps;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.graylog2.indexer.searches.Searches;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldHistogramResult extends HistogramResult {

    private final DateHistogramFacet result;
    private final Searches.DateHistogramInterval interval;

    public FieldHistogramResult(DateHistogramFacet result, String originalQuery, BytesReference builtQuery, Searches.DateHistogramInterval interval, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.result = result;
        this.interval = interval;
    }

    public Searches.DateHistogramInterval getInterval() {
        return interval;
    }

    public Map<Long, Map<String, Object>> getResults() {
        Map<Long, Map<String, Object>> results = Maps.newTreeMap();
        Long minTimestamp = Long.MAX_VALUE;
        Long maxTimestamp = Long.MIN_VALUE;
        for (DateHistogramFacet.Entry e : result) {
            Map<String, Object> resultMap = Maps.newHashMap();

            resultMap.put("count", e.getCount());
            resultMap.put("min", e.getMin());
            resultMap.put("max", e.getMax());
            resultMap.put("total", e.getTotal());
            resultMap.put("total_count", e.getTotalCount());
            resultMap.put("mean", e.getMean());

            final long timestamp = e.getTime() / 1000;
            if (timestamp < minTimestamp) minTimestamp = timestamp;
            if (timestamp > maxTimestamp) maxTimestamp = timestamp;

            results.put(timestamp, resultMap);
        }
        long curTimestamp = minTimestamp;
        while (curTimestamp < maxTimestamp) {
            Map<String, Object> entry = results.get(curTimestamp);

            // advance timestamp by the interval's seconds value
            curTimestamp += interval.getPeriod().toStandardSeconds().getSeconds();

            if (entry != null) {
                continue;
            }
            // synthesize a 0 value for this timestamp
            entry = Maps.newHashMap();
            entry.put("count", 0);
            entry.put("min", 0);
            entry.put("max", 0);
            entry.put("total", 0);
            entry.put("total_count", 0);
            entry.put("mean", 0);
            results.put(curTimestamp, entry);
        }
        return results;
    }

}
