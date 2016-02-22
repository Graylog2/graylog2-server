/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.dashboards.widgets;

import com.google.common.collect.Maps;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.Map;

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
        this.calculatedAt = Tools.nowUTC();
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
