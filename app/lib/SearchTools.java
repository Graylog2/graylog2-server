package lib;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class SearchTools {
	
	public static final Set<String> ALLOWED_DATE_HISTOGRAM_INTERVALS = ImmutableSet.of(
			"year",
			"quarter",
			"month",
			"week",
			"day",
			"hour",
			"minute"
	);
	
    public static String determineDefaultDateHistogramInterval(int timerange) {
    	// All time.
    	if (timerange == 0) {
    		return "month";
    	}
    	
    	// Less than 8 hours.
    	if (timerange <= 8*60*60) {
    		return "minute";
    	}
    	
    	// More than a month.
		if (timerange >= 30*24*60*60) {
			return "day";
		}
		
		// Default.
    	return "hour";
    }
    
    public static boolean isAllowedDateHistogramInterval(String interval) {
    	return ALLOWED_DATE_HISTOGRAM_INTERVALS.contains(interval);
    }
    
}
