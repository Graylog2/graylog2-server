/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
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
package org.graylog2.restclient.models.api.results;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.timeranges.AbsoluteRange;
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.lib.timeranges.TimeRange;

import java.util.List;
import java.util.Map;

public class DateHistogramResult {

	private final String originalQuery;
	private final Map<String, Long> results;
	private final AbsoluteRange histogramBoundaries;
    private final TimeRange timeRange;
	private final String interval;
	private final int tookMs;

	public DateHistogramResult(String originalQuery,
                               int tookMs,
                               String interval,
                               Map<String, Long> results,
                               AbsoluteRange boundaries,
                               TimeRange timeRange) {
		this.originalQuery = originalQuery;
		this.results = results;
		this.interval = interval;
		this.tookMs = tookMs;
        this.histogramBoundaries = boundaries;
        this.timeRange = timeRange;
	}
	
	public Map<String, Long> getResults() {
		return results;
	}
	
	/**
	 * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
	 * 
	 * @return A JSON string representation of the result, suitable for Rickshaw data graphing.
	 */
    @Deprecated
    public List<Map<String, Long>> getFormattedResults() {
		List<Map<String, Long>> points = Lists.newArrayList();
		
		for (Map.Entry<String, Long> result : results.entrySet()) {
			Map<String, Long> point = Maps.newHashMap();
			point.put("x", Long.parseLong(result.getKey()));
			point.put("y", result.getValue());
			
			points.add(point);
		}
		
		return points;
	}

	public String getOriginalQuery() {
		return originalQuery;
	}
	
	public int getTookMs() {
		return tookMs;
	}
	
	public String getInterval() {
		return interval;
	}

    public AbsoluteRange getHistogramBoundaries() {
        return histogramBoundaries;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    /* Indicate if the representation should contain the whole searched time range */
    public boolean hasFixedTimeAxis() {
        return ((timeRange.getType() != TimeRange.Type.RELATIVE) || !(((RelativeRange)timeRange).isEmptyRange()));
    }
}
