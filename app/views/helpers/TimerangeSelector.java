package views.helpers;

import java.util.TreeMap;
import java.util.SortedMap;

import play.mvc.Http.Request;

public class TimerangeSelector {

	@SuppressWarnings("serial")
	public static final SortedMap<Integer, String> ranges = new TreeMap<Integer, String>() {{
		put(5*60, "5 min");
		put(15*60, "15 min");
		put(30*60, "30 min");
		put(60*60, "1 hour");
		put(2*60*60, "2 hours");
		put(8*60*60, "8 hours");
		put(24*60*60, "1 day");
		put(2*24*60*60, "2 days");
		put(5*24*60*60, "5 days");
		put(7*24*60*60, "7 days");
		put(14*24*60*60, "14 days");
		put(30*24*60*60, "30 days");
	}};
	
	private static final String SELECTED = " selected='selected'";
	
	public static String getOptions(Request request) {
		StringBuilder options = new StringBuilder();

		for (SortedMap.Entry<Integer, String> range : ranges.entrySet()) {
			options.append("<option value='").append(range.getKey()).append("'");
			if (isSelected(request, range.getKey())) {
				options.append(SELECTED);
			}
			options.append(">");
			options.append(range.getValue());
			options.append("</option>");
		}
		
		// Special case "All time".
		options.append("<option value='0'");
		if (isSelected(request, 0)) { options.append(SELECTED); }
		options.append(">All time</option>");
		
		return options.toString();
	}
	
	private static boolean isSelected(Request request, int range) {
		String param = request.getQueryString("timerange");
		if (param == null || param.isEmpty()) {
			return false;
		}
		
		return param.equals(String.valueOf(range));
	}
	
}