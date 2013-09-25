package controllers;

import lib.APIException;
import lib.ApiClient;
import lib.SearchTools;
import lib.timeranges.*;
import models.FieldMapper;
import models.UniversalSearch;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;

public class SearchController extends AuthenticatedController {

    public Result index(String q, String rangeType, int relative, String from, String to, String keyword, String interval) {
    	if (q == null || q.isEmpty()) {
    		q = "*";
    	}

        // Histogram interval.
    	if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
    		interval = "hour";
    	}

        Logger.info(rangeType.toUpperCase());

        // Determine timerange type.
        TimeRange.Type timerangeType;
        TimeRange timerange;
        try {
            timerangeType = TimeRange.Type.valueOf(rangeType.toUpperCase());
            switch (timerangeType) {
                case RELATIVE:
                    timerange = new RelativeRange(relative);
                    break;
                case ABSOLUTE:
                    timerange = new AbsoluteRange(from, to);
                    break;
                case KEYWORD:
                    timerange = new KeywordRange(keyword);
                    break;
                default:
                    throw new InvalidRangeParametersException();
            }
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

		try {
			UniversalSearch search = new UniversalSearch(timerange, q);
			SearchResult searchResult = FieldMapper.run(search.search());
			DateHistogramResult histogramResult = search.dateHistogram(interval);

            if (searchResult.getTotalResultCount() > 0) {
			    return ok(views.html.search.results.render(currentUser(), searchResult, histogramResult, q));
            } else {
                return ok(views.html.search.noresults.render(currentUser(), q));
            }
		} catch (IOException e) {
			return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
    }

}
