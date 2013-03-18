package models.api.results;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

public class DateHistogramResult {

	private final String originalQuery;
	private final Map<String, Long> results;
	private final String interval;
	private final int tookMs;

	public DateHistogramResult(String originalQuery, int tookMs, String interval, Map<String, Long> results) {
		this.originalQuery = originalQuery;
		this.results = results;
		this.interval = interval;
		this.tookMs = tookMs;
	}
	
	public Map<String, Long> getResults() {
		return results;
	}
	
	/**
	 * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
	 * 
	 * @return A JSON string representation of the result, suitable for Rickshaw data graphing.
	 */
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
	
	public String asJSONString() {
		return new Gson().toJson(getFormattedResults());
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
	
}
