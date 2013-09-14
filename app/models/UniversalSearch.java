package models;

import lib.APIException;
import lib.ApiClient;
import lib.timeranges.TimeRange;
import models.api.responses.DateHistogramResponse;
import models.api.responses.FieldStatsResponse;
import models.api.responses.SearchResultResponse;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;

import java.io.IOException;

public class UniversalSearch {

    private final String query;
    private final TimeRange timeRange;

    public UniversalSearch(TimeRange timeRange, String query) {
        this.query = query;
        this.timeRange = timeRange;
    }

    public SearchResult search() throws IOException, APIException {
        SearchResultResponse response = ApiClient.get(SearchResultResponse.class)
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
        DateHistogramResponse response = ApiClient.get(DateHistogramResponse.class)
                .path("/search/universal/{0}/histogram", timeRange.getType().toString().toLowerCase())
                .queryParam("interval", interval)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .execute();
        return new DateHistogramResult(response.query, response.time, response.interval, response.results);
    }

    public FieldStatsResponse fieldStats(String field) throws IOException, APIException {
        return ApiClient.get(FieldStatsResponse.class)
                .path("/search/universal/{0}/stats", timeRange.getType().toString().toLowerCase())
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParams(timeRange.getQueryParams())
                .execute();
    }

}
