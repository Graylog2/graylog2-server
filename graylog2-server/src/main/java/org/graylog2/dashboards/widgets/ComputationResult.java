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
package org.graylog2.dashboards.widgets;

import com.google.common.collect.Maps;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ComputationResult {

    private final Object result;
    private final long tookMs;
    private final DateTime calculatedAt;
    private final AbsoluteRange computationTimeRange;

    public ComputationResult(Object result, long tookMs) {
        this(result, tookMs, null);
    }

    public ComputationResult(Object result, long tookMs, AbsoluteRange computationTimeRange) {
        this.result = result;
        this.tookMs = tookMs;
        this.computationTimeRange = computationTimeRange;
        this.calculatedAt = Tools.iso8601();
    }

    public Object getResult() {
        return result;
    }

    public DateTime getCalculatedAt() {
        return calculatedAt;
    }

    public long getTookMs() {
        return tookMs;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("result", result);
        map.put("calculated_at", Tools.getISO8601String(calculatedAt));
        map.put("took_ms", tookMs);

        if (computationTimeRange != null) {
            Map<String, Object> timeRangeMap = Maps.newHashMap();
            timeRangeMap.put("from", Tools.getISO8601String(computationTimeRange.getFrom()));
            timeRangeMap.put("to", Tools.getISO8601String(computationTimeRange.getTo()));
            map.put("computation_time_range", timeRangeMap);
        }

        return map;
    }

}
