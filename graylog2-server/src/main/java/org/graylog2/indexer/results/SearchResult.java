/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.results;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.Message;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SearchResult extends IndexQueryResult {

	private final long totalResults;
	private final List<ResultMessage> results;
	private final Set<String> fields;
    private final Set<IndexRange> usedIndices;

	public SearchResult(List<ResultMessage> hits, long totalResults, Set<IndexRange> usedIndices, String originalQuery, String builtQuery, long tookMs) {
	    super(originalQuery, builtQuery, tookMs);
	    this.results = hits;
        this.fields = extractFields(hits);
        this.totalResults = totalResults;
        this.usedIndices = usedIndices;
    }

    private SearchResult(String query, String originalQuery) {
        super(query, originalQuery, 0);
        this.results = Collections.emptyList();
        this.fields = Collections.emptySet();
        this.usedIndices = Collections.emptySet();
        this.totalResults = 0;
    }

    public long getTotalResults() {
		return totalResults;
	}

	public List<ResultMessage> getResults() {
		return results;
	}

	public Set<String> getFields() {
		return fields;
	}

	@VisibleForTesting
    Set<String> extractFields(List<ResultMessage> hits) {
        Set<String> filteredFields = Sets.newHashSet();

        hits.forEach(hit -> {
            final Message message = hit.getMessage();
            for (String field : message.getFieldNames()) {
                if (!Message.FILTERED_FIELDS.contains(field)) {
                    filteredFields.add(field);
                }
            }
        });

        return filteredFields;
    }

    public Set<IndexRange> getUsedIndices() {
        return usedIndices;
    }

    public static SearchResult empty(String query, String originalQuery) {
        return new SearchResult(query, originalQuery);
    }
}
