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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import controllers.AuthenticatedController;
import lib.SearchTools;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.timeranges.AbsoluteRange;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.UniversalSearch;
import org.graylog2.restclient.models.api.responses.FieldHistogramResponse;
import org.graylog2.restclient.models.api.responses.FieldStatsResponse;
import org.graylog2.restclient.models.api.responses.FieldTermsResponse;
import org.graylog2.restclient.models.api.results.DateHistogramResult;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.MutableDateTime;
import org.joda.time.Weeks;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SearchApiController extends AuthenticatedController {
    private final UniversalSearch.Factory searchFactory;

    @Inject
    public SearchApiController(final UniversalSearch.Factory searchFactory) {
        this.searchFactory = searchFactory;
    }

    public Result fieldStats(String q, String field, String rangeType, int relative, String from, String to, String keyword, String streamId) {
        if (q == null || q.isEmpty()) {
            q = "*";
        }

        // Determine timerange type.
        TimeRange timerange;
        try {
            timerange = TimeRange.factory(rangeType, relative, from, to, keyword);
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
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

            return ok(Json.toJson(result));
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
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
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

            return ok(Json.toJson(result));
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
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
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

            return ok(Json.toJson(result));
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
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        String filter = null;
        if (streamId != null && !streamId.isEmpty()) {
            filter = "streams:" + streamId;
        }

        try {
            UniversalSearch search = searchFactory.queryWithRangeAndFilter(q, timerange, filter);
            DateHistogramResult histogram = search.dateHistogram(interval);
            List<Map<String, Long>> results = formatHistogramResults(histogram, maxDataPoints, relative == 0);

            Map<String, Object> result = Maps.newHashMap();
            AbsoluteRange boundaries = histogram.getHistogramBoundaries();
            result.put("time", histogram.getTookMs());
            result.put("interval", histogram.getInterval());
            result.put("values", results);
            result.put("from", boundaries.getFrom());
            result.put("to", boundaries.getTo());

            return ok(Json.toJson(result));
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
     * Create a list with histogram results that would be serialized to JSON like this
     * <p/>
     * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
     */
    protected List<Map<String, Long>> formatHistogramResults(DateHistogramResult histogram, int maxDataPoints, boolean allQuery) {
        final List<Map<String, Long>> points = Lists.newArrayList();
        final Map<String, Long> histogramResults = histogram.getResults();

        DateTime from;
        if (allQuery) {
            String firstTimestamp = histogramResults.entrySet().iterator().next().getKey();
            from = new DateTime(Long.parseLong(firstTimestamp) * 1000, DateTimeZone.UTC);
        } else {
            from = DateTime.parse(histogram.getHistogramBoundaries().getFrom());
        }
        final DateTime to = DateTime.parse(histogram.getHistogramBoundaries().getTo());
        final MutableDateTime currentTime = new MutableDateTime(from);

        final Duration step = estimateIntervalStep(histogram.getInterval());
        final int dataPoints = (int) ((to.getMillis() - from.getMillis()) / step.getMillis());

        // using the absolute value guarantees, that there will always be enough values for the given resolution
        final int factor = (maxDataPoints != -1 && dataPoints > maxDataPoints) ? dataPoints / maxDataPoints : 1;

        int index = 0;
        floorToBeginningOfInterval(histogram.getInterval(), currentTime);
        while (currentTime.isBefore(to) || currentTime.isEqual(to)) {
            if (index % factor == 0) {
                String timestamp = Long.toString(currentTime.getMillis() / 1000);
                Long result = histogramResults.get(timestamp);
                Map<String, Long> point = Maps.newHashMap();
                point.put("x", Long.parseLong(timestamp));
                point.put("y", result != null ? result : 0);
                points.add(point);
            }
            index++;
            nextStep(histogram.getInterval(), currentTime);
        }

        return points;
    }

    private void nextStep(String interval, MutableDateTime currentTime) {
        switch (interval) {
            case "minute":
                currentTime.addMinutes(1);
                break;
            case "hour":
                currentTime.addHours(1);
                break;
            case "day":
                currentTime.addDays(1);
                break;
            case "week":
                currentTime.addWeeks(1);
                break;
            case "month":
                currentTime.addMonths(1);
                break;
            case "quarter":
                currentTime.addMonths(3);
                break;
            case "year":
                currentTime.addYears(1);
                break;
            default:
                throw new IllegalArgumentException("Invalid duration specified: " + interval);
        }
    }

    private void floorToBeginningOfInterval(String interval, MutableDateTime currentTime) {
        switch (interval) {
            case "minute":
                currentTime.minuteOfDay().roundFloor();
                break;
            case "hour":
                currentTime.hourOfDay().roundFloor();
                break;
            case "day":
                currentTime.dayOfMonth().roundFloor();
                break;
            case "week":
                currentTime.weekOfWeekyear().roundFloor();
                break;
            case "month":
                currentTime.monthOfYear().roundFloor();
                break;
            case "quarter":
                // set the month to the beginning of the quarter
                int currentQuarter = ((currentTime.getMonthOfYear() - 1) / 3);
                int startOfQuarter = (currentQuarter * 3) + 1;
                currentTime.setMonthOfYear(startOfQuarter);
                currentTime.monthOfYear().roundFloor();
                break;
            case "year":
                currentTime.yearOfCentury().roundFloor();
                break;
            default:
                throw new IllegalArgumentException("Invalid duration specified: " + interval);
        }
    }

    private Duration estimateIntervalStep(String interval) {
        Duration step;
        switch (interval) {
            case "minute":
                step = Minutes.ONE.toStandardDuration();
                break;
            case "hour":
                step = Hours.ONE.toStandardDuration();
                break;
            case "day":
                step = Days.ONE.toStandardDuration();
                break;
            case "week":
                step = Weeks.ONE.toStandardDuration();
                break;
            case "month":
                step = Days.days(31).toStandardDuration();
                break;
            case "quarter":
                step = Days.days(31 * 3).toStandardDuration();
                break;
            case "year":
                step = Days.days(365).toStandardDuration();
                break;
            default:
                throw new IllegalArgumentException("Invalid duration specified: " + interval);
        }
        return step;
    }

}
