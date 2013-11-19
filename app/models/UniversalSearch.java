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
package models;

import com.google.common.net.MediaType;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import controllers.routes;
import lib.APIException;
import lib.ApiClient;
import lib.Tools;
import lib.timeranges.TimeRange;
import models.api.responses.*;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;
import play.mvc.Call;
import play.mvc.Http.Request;

import java.io.IOException;

public class UniversalSearch {

    public static final int PER_PAGE = 100;

    private final ApiClient api;
    private final String query;
    private final String filter;
    private final TimeRange timeRange;
    private final Integer page;

    @AssistedInject
    private UniversalSearch(ApiClient api, @Assisted TimeRange timeRange, @Assisted String query) {
        this(api, timeRange, query, 0);
    }

    @AssistedInject
    private UniversalSearch(ApiClient api, @Assisted TimeRange timeRange, @Assisted String query, @Assisted Integer page) {
        this(api, timeRange, query, page, null);
    }

    @AssistedInject
    private UniversalSearch(ApiClient api, @Assisted TimeRange timeRange, @Assisted("query") String query, @Assisted Integer page, @Assisted("filter") String filter) {
        this.filter = filter;
        this.api = api;
        this.query = query;
        this.timeRange = timeRange;

        if (page == 0) {
            this.page = 0;
        } else {
            this.page = page - 1;
        }
    }

    private <T> T doSearch(Class<T> clazz, MediaType mediaType, int pageSize) throws APIException, IOException {
        return api.get(clazz)
                .path("/search/universal/{0}", timeRange.getType().toString().toLowerCase())
                .queryParams(timeRange.getQueryParams())
                .queryParam("query", query)
                .queryParam("limit", pageSize)
                .queryParam("offset", page * pageSize)
                .queryParam("filter", (filter == null ? "*" : filter))
                .accept(mediaType)
                .execute();
    }

    public SearchResult search() throws IOException, APIException {
        SearchResultResponse response = doSearch(SearchResultResponse.class, MediaType.JSON_UTF_8, PER_PAGE);

        SearchResult result = new SearchResult(
                query,
                response.builtQuery,
                timeRange,
                response.total_results,
                response.time,
                response.messages,
                response.fields,
                response.usedIndices
        );

        return result;
    }

    public String searchAsCsv() throws IOException, APIException {
        return doSearch(String.class, MediaType.CSV_UTF_8, 100000);  // TODO fix huge page size by using scroll searches and streaming results
    }

    public DateHistogramResult dateHistogram(String interval) throws IOException, APIException {
        DateHistogramResponse response = api.get(DateHistogramResponse.class)
                .path("/search/universal/{0}/histogram", timeRange.getType().toString().toLowerCase())
                .queryParam("interval", interval)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .execute();
        return new DateHistogramResult(response.query, response.time, response.interval, response.results);
    }

    public FieldStatsResponse fieldStats(String field) throws IOException, APIException {
        return api.get(FieldStatsResponse.class)
                .path("/search/universal/{0}/stats", timeRange.getType().toString().toLowerCase())
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .execute();
    }

    public FieldTermsResponse fieldTerms(String field) throws IOException, APIException {
        return api.get(FieldTermsResponse.class)
                .path("/search/universal/{0}/terms", timeRange.getType().toString().toLowerCase())
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .execute();
    }

    public FieldHistogramResponse fieldHistogram(String field, String interval) throws IOException, APIException {
        return api.get(FieldHistogramResponse.class)
                .path("/search/universal/{0}/fieldhistogram", timeRange.getType().toString().toLowerCase())
                .queryParam("field", field)
                .queryParam("interval", interval)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .queryParam("filter", (filter == null ? "*" : filter))
                .execute();
    }

    public Call getRoute(Request request, int page) {
        int relative = Tools.intSearchParamOrEmpty(request, "relative");
        String from = Tools.stringSearchParamOrEmpty(request, "from");
        String to = Tools.stringSearchParamOrEmpty(request, "to");
        String keyword = Tools.stringSearchParamOrEmpty(request, "keyword");
        String interval = Tools.stringSearchParamOrEmpty(request, "interval");

        return routes.SearchController.index(
                query,
                timeRange.getType().toString().toLowerCase(),
                relative,
                from,
                to,
                keyword,
                interval,
                page
        );
    }

    public Call getCsvRoute(Request request) {
        int relative = Tools.intSearchParamOrEmpty(request, "relative");
        String from = Tools.stringSearchParamOrEmpty(request, "from");
        String to = Tools.stringSearchParamOrEmpty(request, "to");
        String keyword = Tools.stringSearchParamOrEmpty(request, "keyword");

        return routes.SearchController.exportAsCsv(
                query,
                timeRange.getType().toString().toLowerCase(),
                relative,
                from,
                to,
                keyword
        );
    }

    public interface Factory {
        UniversalSearch queryWithRange(String query, TimeRange timeRange);

        UniversalSearch queryWithRangeAndPage(String query, TimeRange timeRange, Integer page);

        UniversalSearch queryWithFilterRangeAndPage(@Assisted("query") String query, @Assisted("filter") String filter, TimeRange timeRange, Integer page);
    }

}
