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

import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.TermsAggregation;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class TermsResult extends IndexQueryResult {

    private final long total;
    private final long missing;
    private final long other;
    private final Map<String, Long> terms;

    public TermsResult(TermsAggregation terms, long missingCount, long totalCount, String originalQuery, String builtQuery, long tookMs) {
        super(originalQuery, builtQuery, tookMs);

        this.total = totalCount;
        this.missing = missingCount;
        this.other = terms.getSumOtherDocCount();
        this.terms = terms.getBuckets().stream()
            .collect(Collectors.toMap(TermsAggregation.Entry::getKey, Bucket::getCount));
    }

    private TermsResult(String originalQuery, String builtQuery) {
        super(originalQuery, builtQuery, 0);

        this.total = 0;
        this.missing = 0;
        this.other = 0;
        this.terms = Collections.emptyMap();
    }

    public static TermsResult empty(String originalQuery, String builtQuery) {
        return new TermsResult(originalQuery, builtQuery);
    }

    public long getTotal() {
        return total;
    }

    public long getMissing() {
        return missing;
    }

    public long getOther() {
        return other;
    }

    public Map<String, Long> getTerms() {
        return terms;
    }

}
