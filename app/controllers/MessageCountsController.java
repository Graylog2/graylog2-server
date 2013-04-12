package controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import lib.APIException;
import models.MessageCount;
import models.api.results.DateHistogramResult;
import play.mvc.*;

public class MessageCountsController extends AuthenticatedController {

	public static Result total(String timerange) {
    	int range;
    	try {
    		range = Integer.parseInt(timerange);
    	} catch (NumberFormatException e) {
    		return badRequest("Invalid timerange.");
    	}
		
		try {
			MessageCount count = new MessageCount("minute", range);
			DateHistogramResult histogramResult = count.total();
			
			List<Map<String, Object>> lines = Lists.newArrayList();
			Map<String, Object> r = Maps.newTreeMap();
			r.put("color", "#26ADE4");
			r.put("name", "Messages");
			r.put("data", histogramResult.getFormattedResults());
			
			lines.add(r);
			
			return ok(new Gson().toJson(lines)).as("application/json");
		} catch (IOException e) {
			return ok("io exception");
		} catch (APIException e) {
			return ok("api exception" + e);
		}
	}
	
}