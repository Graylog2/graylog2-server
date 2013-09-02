package models;

import lib.APIException;
import lib.ApiClient;
import models.api.responses.DateHistogramResponse;
import models.api.responses.FieldStatsResponse;
import models.api.responses.SearchResultResponse;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;

import java.io.IOException;

public class UniversalSearch {

    private final String query;
    private final int timerange;

    public UniversalSearch(String query, int timerange) {
        this.query = query;
        this.timerange = timerange;
    }

    public SearchResult search() throws IOException, APIException {
        SearchResultResponse response = ApiClient.get(SearchResultResponse.class)
                .path("/search/universal")
                .queryParam("query", query)
                .queryParam("timerange", timerange)
                .execute();

        SearchResult result = new SearchResult(
                query,
                timerange,
                response.total_results,
                response.time,
                response.messages,
                response.fields
        );

        return result;
    }

    public DateHistogramResult dateHistogram(String interval) throws IOException, APIException {
        DateHistogramResponse response = ApiClient.get(DateHistogramResponse.class)
                .path("/search/universal/histogram")
                .queryParam("interval", interval)
                .queryParam("query", query)
                .queryParam("timerange", timerange)
                .execute();
        return new DateHistogramResult(response.query, response.time, response.interval, response.results);
    }

    public FieldStatsResponse fieldStats(String field) throws IOException, APIException {
        return ApiClient.get(FieldStatsResponse.class)
                .path("/search/universal/stats")
                .queryParam("field", field)
                .queryParam("query", query)
                .queryParam("timerange", timerange)
                .execute();
    }

}
