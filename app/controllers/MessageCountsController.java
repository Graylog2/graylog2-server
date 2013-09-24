package controllers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import lib.APIException;
import models.MessageCount;
import models.MessageCountHistogram;
import models.api.results.DateHistogramResult;
import models.api.results.MessageCountResult;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MessageCountsController extends AuthenticatedController {

    public Result total() {
        try {
            MessageCount count = new MessageCount();
            MessageCountResult countResult = count.total();

            Map<String, Integer> result = Maps.newHashMap();
            result.put("events", countResult.getEventsCount());

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

	public Result histogram(String timerange) {
    	int range;
    	try {
    		range = Integer.parseInt(timerange);
    	} catch (NumberFormatException e) {
    		return badRequest("Invalid timerange.");
    	}
		
		try {
			MessageCountHistogram count = new MessageCountHistogram("minute", range);
			DateHistogramResult histogramResult = count.histogram();
			
			List<Map<String, Object>> lines = Lists.newArrayList();
			Map<String, Object> r = Maps.newTreeMap();
			r.put("color", "#26ADE4");
			r.put("name", "Messages");
			r.put("data", histogramResult.getFormattedResults());
			
			lines.add(r);
			
			return ok(new Gson().toJson(lines)).as("application/json");
		} catch (IOException e) {
			return internalServerError("io exception");
		} catch (APIException e) {
			return internalServerError("api exception " + e);
		}
	}
	
}