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
