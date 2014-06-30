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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import lib.*;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Field;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.*;
import org.graylog2.restclient.models.api.results.DateHistogramResult;
import org.graylog2.restclient.models.api.results.SearchResult;
import play.mvc.Result;
import views.helpers.Permissions;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SearchController extends AuthenticatedController {

    @Inject
    protected UniversalSearch.Factory searchFactory;

    @Inject
    protected MessagesService messagesService;

    @Inject
    protected SavedSearchService savedSearchService;

    @Inject
    private ServerNodes serverNodes;

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
                        String fields) {
        SearchSort sort = buildSearchSort(sortField, sortOrder);

        UniversalSearch search;
        try {
            search = getSearch(q, null, rangeType, relative, from, to, keyword, page, sort);
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        SearchResult searchResult;
        DateHistogramResult histogramResult;
        SavedSearch savedSearch;
        Set<String> selectedFields = getSelectedFields(fields);
        try {
            if(savedSearchId != null && !savedSearchId.isEmpty()) {
                savedSearch = savedSearchService.get(savedSearchId);
            } else {
                savedSearch = null;
            }

            // Histogram interval.
            if (interval == null || interval.isEmpty() || !SearchTools.isAllowedDateHistogramInterval(interval)) {
                interval = "minute";
            }

            searchResult = search.search();
            if (searchResult.getError() != null) {
                return ok(views.html.search.queryerror.render(currentUser(), q, searchResult, savedSearch, fields, null));
            }
            searchResult.setAllFields(getAllFields());

            histogramResult = search.dateHistogram(interval);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        if (searchResult.getTotalResultCount() > 0) {
            return ok(views.html.search.results.render(currentUser(), search, searchResult, histogramResult, q, page, savedSearch, selectedFields, serverNodes.asMap(), null));
        } else {
            return ok(views.html.search.noresults.render(currentUser(), q, searchResult, savedSearch, selectedFields, null));
        }
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
        } catch(InvalidRangeParametersException e2) {
            return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
        } catch(IllegalArgumentException e1) {
            return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
        }

        final String s;
        try {
            Set<String> selectedFields = getSelectedFields(fields);
            s = search.searchAsCsv(selectedFields);
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

    protected UniversalSearch getSearch(String q, String filter, String rangeType, int relative,String from, String to, String keyword, int page, SearchSort order)
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
        } catch(IllegalArgumentException e) {
            return UniversalSearch.DEFAULT_SORT;
        }
    }
}
