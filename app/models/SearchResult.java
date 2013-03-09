package models;

import java.util.List;

import com.google.common.collect.Lists;

public class SearchResult {
	
	public final String originalQuery;
	public final int totalResultCount;

	public SearchResult(String originalQuery, int totalResultCount) {
		this.originalQuery = originalQuery;
		this.totalResultCount = totalResultCount;
	}
	
	public List<Message> getMessages() {
		return Lists.newArrayList();
	}
	
	public String getOriginalQuery() {
		return originalQuery;
	}
	
	public int getTotalResultCount() {
		return totalResultCount;
	}
	
}
