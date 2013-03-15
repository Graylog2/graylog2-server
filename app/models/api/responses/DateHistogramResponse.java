package models.api.responses;

import java.util.Map;

public class DateHistogramResponse {
	
	public int time;
	public String query;
	public String interval;
	public Map<String, Long> results;
	
}
