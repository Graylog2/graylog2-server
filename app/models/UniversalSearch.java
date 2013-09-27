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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import lib.APIException;
import lib.ApiClient;
import lib.timeranges.TimeRange;
import models.api.responses.DateHistogramResponse;
import models.api.responses.FieldStatsResponse;
import models.api.responses.FieldTermsResponse;
import models.api.responses.SearchResultResponse;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;

import java.io.IOException;

public class UniversalSearch {

    private final ApiClient api;
    private final String query;
    private final TimeRange timeRange;

    @AssistedInject
    private UniversalSearch(ApiClient api, @Assisted TimeRange timeRange, @Assisted String query) {
        this.api = api;
        this.query = query;
        this.timeRange = timeRange;
    }

    public SearchResult search() throws IOException, APIException {
        SearchResultResponse response = api.get(SearchResultResponse.class)
                .path("/search/universal/{0}", timeRange.getType().toString().toLowerCase())
                .queryParams(timeRange.getQueryParams())
                .queryParam("query", query)
                .execute();

        SearchResult result = new SearchResult(
                query,
                timeRange,
                response.total_results,
                response.time,
                response.messages,
                response.fields
        );

        return result;
    }

    public DateHistogramResult dateHistogram(String interval) throws IOException, APIException {
        DateHistogramResponse response = api.get(DateHistogramResponse.class)
                .path("/search/universal/{0}/histogram", timeRange.getType().toString().toLowerCase())
                .queryParam("interval", interval)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .execute();
        return new DateHistogramResult(response.query, response.time, response.interval, response.results);
    }

    public FieldStatsResponse fieldStats(String field) throws IOException, APIException {
        return api.get(FieldStatsResponse.class)
                .path("/search/universal/{0}/stats", timeRange.getType().toString().toLowerCase())
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .execute();
    }

    public FieldTermsResponse fieldTerms(String field) throws IOException, APIException {
        return ApiClient.get(FieldTermsResponse.class)
                .path("/search/universal/{0}/terms", timeRange.getType().toString().toLowerCase())
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .execute();
    }

    public interface Factory {
        UniversalSearch queryWithRange(String query, TimeRange timeRange);
    }

}
