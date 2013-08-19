package controllers;

import java.io.IOException;

import lib.APIException;
import lib.Api;
import lib.SearchTools;
import models.FieldMapper;
import models.UniversalSearch;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;
import play.Logger;
import play.mvc.*;

public class SearchController extends AuthenticatedController {

    public static Result index(String q, String timerange, String interval) {
    	int range;
    	try {
    		range = Integer.parseInt(timerange);
    	} catch (NumberFormatException e) {
    		Logger.warn("Could not parse timerange. Setting to 0.");
    		range = 0;
    	}
    	
    	if (q == null || q.isEmpty()) {
    		q = "*";
    	}
    	
    	if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
    		interval = SearchTools.determineDefaultDateHistogramInterval(range);
    	}
    	
		try {
			UniversalSearch search = new UniversalSearch(q, range);
			SearchResult searchResult = FieldMapper.run(search.search());
			DateHistogramResult histogramResult = search.dateHistogram(interval);

            if (searchResult.getTotalResultCount() > 0) {
			    return ok(views.html.search.results.render(currentUser(), searchResult, histogramResult, q));
            } else {
                return ok(views.html.search.noresults.render(currentUser(), q));
            }
		} catch (IOException e) {
			return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
    }

}
