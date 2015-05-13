/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.MediaType;
import lib.SearchTools;
import lib.security.RestPermissions;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Field;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.MessagesService;
import org.graylog2.restclient.models.SavedSearch;
import org.graylog2.restclient.models.SavedSearchService;
import org.graylog2.restclient.models.SearchSort;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.UniversalSearch;
import org.graylog2.restclient.models.api.responses.QueryParseError;
import org.graylog2.restclient.models.api.results.DateHistogramResult;
import org.graylog2.restclient.models.api.results.SearchResult;
import org.joda.time.Minutes;
import play.libs.Json;
import play.mvc.Result;
import views.helpers.Permissions;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchController extends AuthenticatedController {
    // guess high, so we never have a bad resolution
    private static final int DEFAULT_ASSUMED_GRAPH_RESOLUTION = 4000;

    @Inject
    protected UniversalSearch.Factory searchFactory;
    @Inject
    protected MessagesService messagesService;
    @Inject
    protected SavedSearchService savedSearchService;
    @Inject
    private ServerNodes serverNodes;
    @Inject
    private ObjectMapper objectMapper;

    public Result globalSearch() {
        // User would not be allowed to do any global searches anyway, so we can redirect him to the streams page to avoid confusion.
        if (Permissions.isPermitted(RestPermissions.SEARCHES_ABSOLUTE)
                || Permissions.isPermitted(RestPermissions.SEARCHES_RELATIVE)
                || Permissions.isPermitted(RestPermissions.SEARCHES_KEYWORD)) {
            return ok(views.html.search.global.render(currentUser()));
        } else {
            return redirect(routes.StreamsController.index());
        }
    }

    public Result index(String q,
                        String rangeType,
                        int relative,
                        String from, String to,
                        String keyword,
                        String interval,
                        int page,
                        String savedSearchId,
                        String sortField, String sortOrder,
                        String fields,
                        int displayWidth) {
        SearchSort sort = buildSearchSort(sortField, sortOrder);

        return renderSearch(q, rangeType, relative, from, to, keyword, interval, page, savedSearchId, fields, displayWidth, sort, null, null);
    }

    protected Result renderSearch(String q, String rangeType, int relative, String from, String to, String keyword, String interval, int page, String savedSearchId, String fields, int displayWidth, SearchSort sort, Stream stream, String filter) {
        UniversalSearch search;
        try {
            search = getSearch(q, filter, rangeType, relative, from, to, keyword, page, sort);
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        SearchResult searchResult;
        DateHistogramResult histogramResult;
        SavedSearch savedSearch = null;
        Set<String> selectedFields = getSelectedFields(fields);
        String formattedHistogramResults;

        try {
            if (savedSearchId != null && !savedSearchId.isEmpty()) {
                savedSearch = savedSearchService.get(savedSearchId);
            }

            searchResult = search.search();
            searchResult.setAllFields(getAllFields());

            // histogram resolution (strangely aka interval)
            if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
                interval = determineHistogramResolution(searchResult);
            }
            histogramResult = search.dateHistogram(interval);
            formattedHistogramResults = formatHistogramResults(histogramResult.getResults(), displayWidth);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                try {
                    QueryParseError qpe = objectMapper.readValue(e.getResponseBody(), QueryParseError.class);
                    return ok(views.html.search.queryerror.render(currentUser(), q, qpe, savedSearch, fields, stream));
                } catch (IOException ioe) {
                    // Ignore
                }
            }

            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        if (searchResult.getTotalResultCount() > 0) {
            return ok(views.html.search.results.render(currentUser(), search, searchResult, histogramResult, formattedHistogramResults, q, page, savedSearch, selectedFields, serverNodes.asMap(), stream));
        } else {
            return ok(views.html.search.noresults.render(currentUser(), q, searchResult, savedSearch, selectedFields, stream));
        }
    }

    protected String determineHistogramResolution(final SearchResult searchResult) {
        final String interval;
        final int HOUR = 60;
        final int DAY = HOUR * 24;
        final int WEEK = DAY * 7;
        final int MONTH = HOUR * 24 * 30;
        final int YEAR = MONTH * 12;

        // Return minute as default resolution if search from and to DateTimes are not available
        if (searchResult.getFromDateTime() == null && searchResult.getToDateTime() == null) {
            return "minute";
        }

        int queryRangeInMinutes;

        // We don't want to use fromDateTime coming from the search query if the user asked for all messages
        if (isEmptyRelativeRange(searchResult.getTimeRange())) {
            List<IndexRangeSummary> usedIndices = searchResult.getUsedIndices();
            Collections.sort(usedIndices, new Comparator<IndexRangeSummary>() {
                @Override
                public int compare(IndexRangeSummary o1, IndexRangeSummary o2) {
                    return o1.start().compareTo(o2.start());
                }
            });
            IndexRangeSummary oldestIndex = usedIndices.get(0);
            queryRangeInMinutes = Minutes.minutesBetween(oldestIndex.start(), searchResult.getToDateTime()).getMinutes();
        } else {
            queryRangeInMinutes = Minutes.minutesBetween(searchResult.getFromDateTime(), searchResult.getToDateTime()).getMinutes();
        }

        if (queryRangeInMinutes < DAY / 2) {
            interval = "minute";
        } else if (queryRangeInMinutes < DAY * 2) {
            interval = "hour";
        } else if (queryRangeInMinutes < MONTH) {
            interval = "day";
        } else if (queryRangeInMinutes < MONTH * 6) {
            interval = "week";
        } else if (queryRangeInMinutes < YEAR * 2) {
            interval = "month";
        } else if (queryRangeInMinutes < YEAR * 10) {
            interval = "quarter";
        } else {
            interval = "year";
        }
        return interval;
    }

    private boolean isEmptyRelativeRange(TimeRange timeRange) {
        return (timeRange.getType() == TimeRange.Type.RELATIVE) && (((RelativeRange) timeRange).isEmptyRange());
    }

    /**
     * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
     *
     * @return A JSON string representation of the result, suitable for Rickshaw data graphing.
     */
    protected String formatHistogramResults(Map<String, Long> histogramResults, int displayWidth) {
        final int saneDisplayWidth = (displayWidth == -1 || displayWidth < 100 || displayWidth > DEFAULT_ASSUMED_GRAPH_RESOLUTION) ? DEFAULT_ASSUMED_GRAPH_RESOLUTION : displayWidth;
        final List<Map<String, Long>> points = Lists.newArrayList();

        // using the absolute value guarantees, that there will always be enough values for the given resolution
        final int factor = (saneDisplayWidth != -1 && histogramResults.size() > saneDisplayWidth) ? histogramResults.size() / saneDisplayWidth : 1;

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

        return Json.stringify(Json.toJson(points));
    }

    protected Set<String> getSelectedFields(String fields) {
        Set<String> selectedFields = Sets.newLinkedHashSet();
        if (fields != null && !fields.isEmpty()) {
            Iterables.addAll(selectedFields, Splitter.on(',').split(fields));
        } else {
            selectedFields.addAll(Field.STANDARD_SELECTED_FIELDS);
        }
        return selectedFields;
    }

    public Result exportAsCsv(String q, String filter, String rangeType, int relative, String from, String to, String keyword, String fields) {
        UniversalSearch search;
        try {
            search = getSearch(q, filter.isEmpty() ? null : filter, rangeType, relative, from, to, keyword, 0, UniversalSearch.DEFAULT_SORT);
        } catch (InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch (IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        final String s = "obsolete";
//        try {
            Set<String> selectedFields = getSelectedFields(fields);
            //s = search.searchAsCsv(selectedFields);
//        } catch (IOException e) {
//            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
//        } catch (APIException e) {
//            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
//            return status(504, views.html.errors.error.render(message, e, request()));
//        }

        // TODO streaming the result
        response().setContentType(MediaType.CSV_UTF_8.toString());
        response().setHeader("Content-Disposition", "attachment; filename=graylog-searchresult.csv");
        return ok(s);
    }

    protected List<Field> getAllFields() {
        List<Field> allFields = Lists.newArrayList();
        for (String f : messagesService.getMessageFields()) {
            allFields.add(new Field(f));
        }
        return allFields;
    }

    protected UniversalSearch getSearch(String q, String filter, String rangeType, int relative, String from, String to, String keyword, int page, SearchSort order)
            throws InvalidRangeParametersException, IllegalArgumentException {
        if (q == null || q.trim().isEmpty()) {
            q = "*";
        }

        // Determine timerange type.
        TimeRange timerange = TimeRange.factory(rangeType, relative, from, to, keyword);

        UniversalSearch search;
        if (filter == null) {
            search = searchFactory.queryWithRangePageAndOrder(q, timerange, page, order);
        } else {
            search = searchFactory.queryWithFilterRangePageAndOrder(q, filter, timerange, page, order);
        }

        return search;
    }

    protected SearchSort buildSearchSort(String sortField, String sortOrder) {
        if (sortField == null || sortOrder == null || sortField.isEmpty() || sortOrder.isEmpty()) {
            return UniversalSearch.DEFAULT_SORT;
        }

        try {
            return new SearchSort(sortField, SearchSort.Direction.valueOf(sortOrder.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return UniversalSearch.DEFAULT_SORT;
        }
    }
}
