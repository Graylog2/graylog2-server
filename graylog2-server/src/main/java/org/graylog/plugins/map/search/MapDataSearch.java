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
package org.graylog.plugins.map.search;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.rest.models.search.responses.TermsResult;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class MapDataSearch {
    private static final Pattern VALIDATION_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");

    private final Searches searches;

    @Inject
    public MapDataSearch(final Searches searches) {
        this.searches = searches;
    }

    public MapDataSearchResult searchMapData(final MapDataSearchRequest request) throws ValueTypeException {
        final ImmutableMap.Builder<String, TermsResult> termResults = ImmutableMap.<String, TermsResult>builder();

        String filter = null;
        if (!isNullOrEmpty(request.streamId())) {
            filter = "streams:" + request.streamId();
        }

        for (final String field : request.fields()) {
            final org.graylog2.indexer.results.TermsResult terms = searches.terms(field, request.limit(), request.query(), filter, request.timerange());
            // TODO: Validate data!
            final Map<String, Long> validatedTerms = validateTerms(field, terms.getTerms());
            final TermsResult result = TermsResult.create(terms.tookMs(), validatedTerms, terms.getMissing(), terms.getOther(), terms.getTotal(), terms.getBuiltQuery());
            termResults.put(field, result);
        }

        return MapDataSearchResult.builder()
                .query(request.query())
                .timerange(request.timerange())
                .limit(request.limit())
                .streamId(request.streamId())
                .fields(termResults.build())
                .build();
    }

    private Map<String, Long> validateTerms(final String field, final Map<String, Long> terms) throws ValueTypeException {
        final Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();

        for (String term : terms.keySet()) {
            if (Strings.isNullOrEmpty(term)) {
                continue;
            }

            final List<String> list = splitter.splitToList(term);

            if (list.size() != 2) {
                throw getValueTypeException(field, term);
            }

            for (String value : list) {
                if (!VALIDATION_PATTERN.matcher(value).matches()) {
                    throw getValueTypeException(field, term);
                }
            }
        }

        return terms;
    }

    private ValueTypeException getValueTypeException(final String field, final String term) {
        return new ValueTypeException("Invalid geo data term for field \"" + field + "\": " + term + " (required: <lat>,<lng> - example: 1.23,3.11)");
    }

    public static class ValueTypeException extends Exception {
        public ValueTypeException(final String message) {
            super(message);
        }
    }
}
