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
package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import lib.SearchTools;
import org.graylog2.restclient.lib.timeranges.*;
import org.graylog2.restclient.models.UniversalSearch;
import org.graylog2.restclient.models.api.responses.FieldHistogramResponse;
import org.graylog2.restclient.models.api.responses.FieldStatsResponse;
import org.graylog2.restclient.models.api.responses.FieldTermsResponse;
import org.graylog2.restclient.models.api.results.DateHistogramResult;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchApiController extends AuthenticatedController {

    @Inject
    private UniversalSearch.Factory searchFactory;

    public Result fieldStats(String q, String field, String rangeType, int relative, String from, String to, String keyword, String streamId) {
        if (q == null || q.isEmpty()) {
            q = "*";
        }

        // Determine timerange type.
        TimeRange timerange;
        try {
            timerange = TimeRange.factory(rangeType, relative, from, to, keyword);
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        String filter = null;
        if (streamId != null && !streamId.isEmpty()) {
            filter = "streams:" + streamId;
        }

        try {
            UniversalSearch search = searchFactory.queryWithRangeAndFilter(q, timerange, filter);
            FieldStatsResponse stats = search.fieldStats(field);

            Map<String, Object> result = Maps.newHashMap();
            result.put("count", stats.count);
            result.put("sum", stats.sum);
            result.put("mean", stats.mean);
            result.put("min", stats.min);
            result.put("max", stats.max);
            result.put("variance", stats.variance);
            result.put("sum_of_squares", stats.sumOfSquares);
            result.put("std_deviation", stats.stdDeviation);

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                // This usually means the field does not have a numeric type. Pass through!
                return badRequest();
            }

            return internalServerError("api exception " + e);
        }
    }

    public Result fieldTerms(String q, String field, String rangeType, int relative, String from, String to, String keyword, String streamId) {
        if (q == null || q.isEmpty()) {
            q = "*";
        }

        // Determine timerange type.
        TimeRange timerange;
        try {
            timerange = TimeRange.factory(rangeType, relative, from, to, keyword);
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        String filter = null;
        if (streamId != null && !streamId.isEmpty()) {
            filter = "streams:" + streamId;
        }

        try {
            UniversalSearch search = searchFactory.queryWithRangeAndFilter(q, timerange, filter);
            FieldTermsResponse terms = search.fieldTerms(field);

            Map<String, Object> result = Maps.newHashMap();
            result.put("total", terms.total);
            result.put("missing", terms.missing);
            result.put("time", terms.time);
            result.put("other", terms.other);
            result.put("terms", terms.terms);

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                // This usually means the field does not have a numeric type. Pass through!
                return badRequest();
            }

            return internalServerError("api exception " + e);
        }
    }


    public Result fieldHistogram(String q, String field, String rangeType, int relative, String from, String to, String keyword, String interval, String valueType, String streamId) {
        if (q == null || q.isEmpty()) {
            q = "*";
        }

        // Interval.
        if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
            interval = "hour";
        }

        // Determine timerange type.
        TimeRange timerange;
        try {
            timerange = TimeRange.factory(rangeType, relative, from, to, keyword);
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        String filter = null;
        if (streamId != null && !streamId.isEmpty()) {
            filter = "streams:" + streamId;
        }

        try {
            UniversalSearch search = searchFactory.queryWithRangeAndFilter(q, timerange, filter);
            FieldHistogramResponse histo = search.fieldHistogram(field, interval);

            Map<String, Object> result = Maps.newHashMap();
            AbsoluteRange boundaries = histo.getHistogramBoundaries();
            result.put("time", histo.time);
            result.put("interval", histo.interval);
            result.put("values", histo.getFormattedResults(valueType));
            result.put("from", boundaries.getFrom());
            result.put("to", boundaries.getTo());

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                // This usually means the field does not have a numeric type. Pass through!
                return badRequest();
            }

            return internalServerError("api exception " + e);
        }
    }

    public Result histogram(String q, String rangeType, int relative, String from, String to, String keyword, String interval, String streamId, int maxDataPoints) {
        if (q == null || q.isEmpty()) {
            q = "*";
        }

        // Interval.
        if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
            interval = "minute";
        }

        // Determine timerange type.
        TimeRange timerange;
        try {
            timerange = TimeRange.factory(rangeType, relative, from, to, keyword);
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        String filter = null;
        if (streamId != null && !streamId.isEmpty()) {
            filter = "streams:" + streamId;
        }

        try {
            UniversalSearch search = searchFactory.queryWithRangeAndFilter(q, timerange, filter);
            DateHistogramResult histo = search.dateHistogram(interval);
            List<Map<String, Long>> results = formatHistogramResults(histo.getResults(), maxDataPoints);

            Map<String, Object> result = Maps.newHashMap();
            AbsoluteRange boundaries = histo.getHistogramBoundaries();
            result.put("time", histo.getTookMs());
            result.put("interval", histo.getInterval());
            result.put("values", results);
            result.put("from", boundaries.getFrom());
            result.put("to", boundaries.getTo());

            ObjectMapper om = new ObjectMapper();
            String json = om.writeValueAsString(result);

            return ok(json).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                // This usually means the field does not have a numeric type. Pass through!
                return badRequest();
            }

            return internalServerError("api exception " + e);
        }
    }

    /**
     * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
     *
     */
    protected List<Map<String, Long>> formatHistogramResults(Map<String, Long> histogramResults, int maxDataPoints) {
        final List<Map<String, Long>> points = Lists.newArrayList();

        // using the absolute value guarantees, that there will always be enough values for the given resolution
        final int factor = (maxDataPoints != -1 && histogramResults.size() > maxDataPoints) ? histogramResults.size() / maxDataPoints : 1;

        int index = 0;
        for (Map.Entry<String, Long> result : histogramResults.entrySet()) {
            // TODO: instead of sampling we might consider interpolation (compare DashboardsApiController)
            if (index % factor == 0) {
                Map<String, Long> point = Maps.newHashMap();
                point.put("x", Long.parseLong(result.getKey()));
                point.put("y", result.getValue());

                points.add(point);
            }
            index++;
        }

        return points;
    }

}
