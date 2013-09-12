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

    public static boolean isAllowedDateHistogramInterval(String interval) {
    	return ALLOWED_DATE_HISTOGRAM_INTERVALS.contains(interval);
    }
    
}
