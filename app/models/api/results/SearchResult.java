package models.api.results;

import java.util.List;

import com.google.common.collect.Lists;

import lib.Field;
import models.api.responses.MessageSummaryResponse;

public class SearchResult {
	
	private final String originalQuery;
    private final int originalTimerange;
	private final int totalResultCount;
	private final int tookMs;
	private final List<MessageSummaryResponse> results;
	private final List<Field> fields;

	public SearchResult(String originalQuery, int timerange, int totalResultCount, int tookMs, List<MessageSummaryResponse> results, List<String> fields) {
		this.originalQuery = originalQuery;
        this.originalTimerange = timerange;
		this.totalResultCount = totalResultCount;
		this.tookMs = tookMs;
		this.results = results;
		this.fields = buildFields(fields);
	}
	
	public List<MessageSummaryResponse> getMessages() {
		return results;
	}
	
	public String getOriginalQuery() {
		return originalQuery;
	}

    public int getOriginalTimerange() {
        return originalTimerange;
    }

    public int getTookMs() {
		return tookMs;
	}
	
	public int getTotalResultCount() {
		return totalResultCount;
	}
	
	public List<Field> getFields() {
		return fields;
	}
	
	private List<Field> buildFields(List<String> sFields) {
		List<Field> fields = Lists.newArrayList();

        if (sFields != null) {
            for (String field : sFields) {
                fields.add(new Field(field));
            }
        }
		
		return fields;
	}
	
}
