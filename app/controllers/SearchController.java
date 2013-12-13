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

import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.Field;
import lib.SearchTools;
import lib.timeranges.InvalidRangeParametersException;
import lib.timeranges.TimeRange;
import models.FieldMapper;
import models.MessagesService;
import models.UniversalSearch;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;

public class SearchController extends AuthenticatedController {

    @Inject
    protected UniversalSearch.Factory searchFactory;
    @Inject
    protected MessagesService messagesService;

    public Result index(String q, String rangeType, int relative, String from, String to, String keyword, String interval, int page) {
        UniversalSearch search;
        try {
            search = getSearch(q, null, rangeType, relative, from, to, keyword, page);
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        SearchResult searchResult;
        DateHistogramResult histogramResult;
        try {
            // Histogram interval.
            if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
                interval = "minute";
            }

            searchResult = FieldMapper.run(search.search());

            searchResult.setAllFields(getAllFields());

            histogramResult = search.dateHistogram(interval);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        if (searchResult.getTotalResultCount() > 0) {
            return ok(views.html.search.results.render(currentUser(), search, searchResult, histogramResult, q, page, null));
        } else {
            return ok(views.html.search.noresults.render(currentUser(), q, searchResult, null));
        }
    }

    public Result exportAsCsv(String q, String rangeType, int relative, String from, String to, String keyword) {
        UniversalSearch search;
        try {
            search = getSearch(q, null, rangeType, relative, from, to, keyword, 0);
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        final String s;
        try {
            s = search.searchAsCsv();
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        // TODO streaming the result
        response().setContentType(MediaType.CSV_UTF_8.toString());
        response().setHeader("Content-Disposition", "attachment; filename=graylog2-searchresult.csv");
        return ok(s);
    }

    protected List<Field> getAllFields() {
        List<Field> allFields = Lists.newArrayList();
        for(String f : messagesService.getMessageFields()) {
            allFields.add(new Field(f));
        }
        return allFields;
    }

    protected UniversalSearch getSearch(String q, String filter, String rangeType, int relative,String from, String to, String keyword, int page)
        throws InvalidRangeParametersException, IllegalArgumentException {
        if (q == null || q.trim().isEmpty()) {
            q = "*";
        }

        // Determine timerange type.
        TimeRange timerange = TimeRange.factory(rangeType, relative, from, to, keyword);

        UniversalSearch search;
        if (filter == null)
            search= searchFactory.queryWithRangeAndPage(q, timerange, page);
        else
            search = searchFactory.queryWithFilterRangeAndPage(q, filter, timerange, page);

        return search;
    }
}
