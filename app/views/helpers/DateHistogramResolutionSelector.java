package views.helpers;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.text.WordUtils;

import lib.SearchTools;
import play.mvc.Http;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class DateHistogramResolutionSelector {

	public static String getOptions(String selected, Http.Request request) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String interval : SearchTools.ALLOWED_DATE_HISTOGRAM_INTERVALS) {
            StringBuilder url = new StringBuilder(request.path());

            Map<String, String[]> queryParams = Maps.newHashMap(request.queryString());
            String[] qryInterval = {interval};
            queryParams.put("interval", qryInterval);

            url.append("?");

            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {

                for (String value : entry.getValue()) {
                    try {
                        url.append(entry.getKey())
                                .append("=")
                                .append(java.net.URLEncoder.encode(value, Charsets.UTF_8.name()))
                                .append("&");
                    } catch (UnsupportedEncodingException e) {
                        // UTF-8 *is* supported, but just in case...
                        throw new RuntimeException(e);
                    }
                }
            }

            String finalUrl = url.substring(0, url.length()-1);

            String clazz = "date-histogram-res-selector";
            if (interval.equals(selected)) {
                clazz += " selected-resolution";
            }

            sb.append("<a href='").append(finalUrl).append("' class='").append(clazz).append("' data-resolution='").append(interval).append("'>");
			sb.append(WordUtils.capitalize(interval));
			sb.append("</a>");
			
			if (i != SearchTools.ALLOWED_DATE_HISTOGRAM_INTERVALS.size()-1) {
				sb.append(", ");
			}
				
			i++;
		}
		
		return sb.toString();
	}
	
}
