package models;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import lib.APIException;
import lib.Api;
import models.api.responses.SearchResultResponse;

import com.google.common.collect.Lists;

public class UniversalSearch {

	private final String query;
	
	public UniversalSearch(String query) {
		try {
			this.query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported Encoding");
		}
	}
	
	public SearchResult execute() throws IOException, APIException {
		URL url = new URL("http://localhost:12900/search/universal?query=" + query);
		
		SearchResultResponse result = Api.get(url, new SearchResultResponse());

		List<Message> messages = Lists.newArrayList();
		SearchResult r = new SearchResult(result.query, result.total_results, result.time, messages);
		return r;
	}
	
}
