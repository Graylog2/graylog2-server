package models;

import java.io.IOException;
import java.net.URL;

import lib.APIException;
import lib.Api;
import models.api.responses.DateHistogramResponse;
import models.api.responses.SearchResultResponse;

public class UniversalSearch {

	private final String query;
	
	public UniversalSearch(String query) {
		this.query = Api.urlEncode(query);
	}
	
	public SearchResult search() throws IOException, APIException {
		URL url = Api.buildTarget("search/universal?query=" + query);
		
		SearchResultResponse response = Api.get(url, new SearchResultResponse());
		SearchResult result = new SearchResult(response.query, response.total_results, response.time, response.messages);
		return result;
	}
	
	public DateHistogramResult dateHistogram(String interval) throws IOException, APIException {
		String i = Api.urlEncode(interval);
		URL url = Api.buildTarget("search/universal/histogram?interval=" + i + "&query=" + query);
		
		DateHistogramResponse response = Api.get(url, new DateHistogramResponse());
		return new DateHistogramResult(response.query, response.time, response.interval, response.results);
	}
	
}
