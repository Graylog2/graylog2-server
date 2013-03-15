package models;

import java.util.List;

import models.api.responses.MessageSummaryResponse;

public class SearchResult {
	
	private final String originalQuery;
	private final int totalResultCount;
	private final int tookMs;
	private final List<MessageSummaryResponse> results;

	public SearchResult(String originalQuery, int totalResultCount, int tookMs, List<MessageSummaryResponse> results) {
		this.originalQuery = originalQuery;
		this.totalResultCount = totalResultCount;
		this.tookMs = tookMs;
		this.results = results;
	}
	
	public List<MessageSummaryResponse> getMessages() {
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
