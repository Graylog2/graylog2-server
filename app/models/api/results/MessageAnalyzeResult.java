package models.api.results;

import java.util.List;

public class MessageAnalyzeResult {
	
	private final List<String> tokens;
	
	public MessageAnalyzeResult(List<String> tokens) {
		this.tokens = tokens;
	}
	
	public List<String> getTokens() {
		return tokens;
	}
	
}
