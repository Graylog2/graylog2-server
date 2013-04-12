package models.api.responses;

import java.util.List;

public class SearchResultResponse {
	
	public int time;
	public String query;
	public int total_results;
	public List<MessageSummaryResponse> messages;
	public List<String> fields;
	
}
