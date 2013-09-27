/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package models.api.results;

import java.util.List;

import com.google.common.collect.Lists;

import lib.Field;
import lib.timeranges.TimeRange;
import models.api.responses.MessageSummaryResponse;

public class SearchResult {
	
	private final String originalQuery;
    private final TimeRange timeRange;
	private final int totalResultCount;
	private final int tookMs;
	private final List<MessageSummaryResponse> results;
	private final List<Field> fields;

	public SearchResult(String originalQuery, TimeRange timeRange, int totalResultCount, int tookMs, List<MessageSummaryResponse> results, List<String> fields) {
		this.originalQuery = originalQuery;
        this.timeRange = timeRange;
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

    public TimeRange getTimeRange() {
        return timeRange;
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
