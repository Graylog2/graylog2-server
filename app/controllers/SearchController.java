/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.SearchTools;
import lib.timeranges.*;
import models.FieldMapper;
import models.UniversalSearch;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;
import play.mvc.Result;

import java.io.IOException;

public class SearchController extends AuthenticatedController {

    @Inject
    private UniversalSearch.Factory searchFactory;

    public Result index(String q, String rangeType, int relative, String from, String to, String keyword, String interval) {
    	if (q == null || q.isEmpty()) {
    		q = "*";
    	}

        // Histogram interval.
    	if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
    		interval = "hour";
    	}

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
			UniversalSearch search = searchFactory.queryWithRange(q, timerange);
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
