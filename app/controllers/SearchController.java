package controllers;

import java.io.IOException;

import lib.APIException;
import lib.SearchTools;
import models.DateHistogramResult;
import models.SearchResult;
import models.UniversalSearch;
import play.mvc.*;

public class SearchController extends AuthenticatedController {

    public static Result index(String q, String timerange, String interval) {
    	int range;
    	try {
    		range = Integer.parseInt(timerange);
    	} catch (NumberFormatException e) {
    		range = 60*60;
    	}
    	
    	if (interval == null || interval.isEmpty()) {
    		interval = SearchTools.determineHistogramInterval(range);
    	}
    	
		try {
			UniversalSearch search = new UniversalSearch(q);
			SearchResult searchResult = search.search();
			DateHistogramResult histogramResult = search.dateHistogram(interval);

			return ok(views.html.search.results.render(currentUser(), searchResult, histogramResult, q));
		} catch (IOException e) {
			String message = "Could not connect to graylog2-server. Please make sure that it is running and you " +
					"configured the correct REST URI.";
			return status(504, views.html.errors.error.render(message, e, request()));
		} catch (APIException e) {
			String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
    }

}
