/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
