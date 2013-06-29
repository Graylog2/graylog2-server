package models;

import java.io.IOException;
import java.net.URL;

import lib.APIException;
import lib.Api;
import models.api.responses.DateHistogramResponse;
import models.api.responses.SearchResultResponse;
import models.api.results.DateHistogramResult;
import models.api.results.SearchResult;

public class UniversalSearch {

	private final String query;
	private final int timerange;
	
	public UniversalSearch(String query, int timerange) {
		this.query = Api.urlEncode(query);
		this.timerange = timerange;
	}
	
	public SearchResult search() throws IOException, APIException {
		String resource = "search/universal?query=" + query + "&timerange=" + timerange;
		
		SearchResultResponse response = Api.get(resource, SearchResultResponse.class);
		SearchResult result = new SearchResult(
				response.query,
				response.total_results,
				response.time,
				response.messages,
				response.fields
		);
		
		return result;
	}
	
	public DateHistogramResult dateHistogram(String interval) throws IOException, APIException {
		String i = Api.urlEncode(interval);
        String resource = "search/universal/histogram?interval=" + i + "&query=" + query + "&timerange=" + timerange;
		
		DateHistogramResponse response = Api.get(resource, DateHistogramResponse.class);
		return new DateHistogramResult(response.query, response.time, response.interval, response.results);
	}
	
}
