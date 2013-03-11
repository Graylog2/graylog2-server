package models;

import java.util.List;

public class SearchResult {
	
	private final String originalQuery;
	private final int totalResultCount;
	private final int tookMs;
	private final List<Message> results;

	public SearchResult(String originalQuery, int totalResultCount, int tookMs, List<Message> results) {
		this.originalQuery = originalQuery;
		this.totalResultCount = totalResultCount;
		this.tookMs = tookMs;
		this.results = results;
	}
	
	public List<Message> getMessages() {
		return results;
	}
	
	public String getOriginalQuery() {
		return originalQuery;
	}
	
	public int getTookMs() {
		return tookMs;
	}
	
	public int getTotalResultCount() {
		return totalResultCount;
	}
	
}
