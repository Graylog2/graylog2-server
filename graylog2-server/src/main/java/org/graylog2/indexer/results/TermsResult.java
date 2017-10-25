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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.graylog2.indexer.searches.Searches;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TermsResult extends IndexQueryResult {

    private final long total;
    private final long missing;
    private final long other;
    private final Map<String, Long> terms = new HashMap<>();
    private final Map<String, List<Map<String, String>>> termsMapping = new HashMap<>();

    public TermsResult(TermsAggregation terms, long missingCount, long totalCount, String originalQuery, String builtQuery, long tookMs) {
        this(terms, missingCount, totalCount, originalQuery, builtQuery, tookMs, Collections.emptyList());
    }

    public TermsResult(TermsAggregation terms, long missingCount, long totalCount, String originalQuery, String builtQuery, long tookMs, List<String> fields) {
        super(originalQuery, builtQuery, tookMs);

        this.total = totalCount;
        this.missing = missingCount;
        this.other = terms.getSumOtherDocCount();

        processTermsBuckets(terms, fields, this.terms, this.termsMapping);
    }

    private TermsResult(String originalQuery, String builtQuery) {
        super(originalQuery, builtQuery, 0);

        this.total = 0;
        this.missing = 0;
        this.other = 0;
    }

    private static void processTermsBuckets(TermsAggregation buckets,
                                            List<String> fields,
                                            Map<String, Long> terms,
                                            Map<String, List<Map<String, String>>> termsMapping) {
        for (final TermsAggregation.Entry entry : buckets.getBuckets()) {
            // Use the "special" character to split up the terms value so we can create a field->value mapping.
            final List<String> valueList = Splitter.on(Searches.STACKED_TERMS_AGG_SEPARATOR).splitToList(entry.getKey());

            // We need a human readable value here because the word separator we are using might not be readable
            final String value = entry.getKey().replace(Searches.STACKED_TERMS_AGG_SEPARATOR, " - ");

            // For every field in the field list, get the value from the split up terms value list. After this, we
            // have a mapping of field->value for each bucket.
            final ImmutableList.Builder<Map<String, String>> mapping = ImmutableList.builder();
            for (int i = 0; i < fields.size(); i++) {
                mapping.add(ImmutableMap.of("field", fields.get(i), "value", valueList.get(i)));
            }

            terms.put(value, entry.getCount());
            termsMapping.put(value, mapping.build());
        }
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

    public Map<String, List<Map<String, String>>> termsMapping() {
        return termsMapping;
    }
}
