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
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.Message;

import java.util.List;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SearchResult extends IndexQueryResult {

	private final long totalResults;
	private final List<ResultMessage> results;
	private final Set<String> fields;
    private final Set<IndexRange> usedIndices;

	public SearchResult(SearchHits searchHits, Set<IndexRange> usedIndices, String originalQuery, BytesReference builtQuery, TimeValue took) {
        super(originalQuery, builtQuery, took);

		this.results = buildResults(searchHits);
		this.fields = extractFields(results);
		this.totalResults = searchHits.getTotalHits();
        this.usedIndices = usedIndices;
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

}
