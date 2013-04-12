package views.helpers;

import org.apache.commons.lang3.text.WordUtils;

import lib.SearchTools;

public class DateHistogramResolutionSelector {

	public static String getOptions(String selected) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String interval : SearchTools.ALLOWED_DATE_HISTOGRAM_INTERVALS) {
			if (interval.equals(selected)) { sb.append("<strong>"); }
			
			sb.append("<a href='#' class='date-histogram-res-selector' data-resolution='").append(interval).append("'>");
			sb.append(WordUtils.capitalize(interval));
			sb.append("</a>");
			
			if (interval.equals(selected)) { sb.append("</strong>"); }
			
			if (i != SearchTools.ALLOWED_DATE_HISTOGRAM_INTERVALS.size()-1) {
				sb.append(", ");
			}
				
			i++;
		}
		
		return sb.toString();
	}
	
}
