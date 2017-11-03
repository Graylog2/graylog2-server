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

import com.google.common.collect.Maps;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.MissingAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TermsHistogramResult extends IndexQueryResult {
    private final long size;
    private final Searches.DateHistogramInterval interval;
    private final Map<Long, TermsResult> result;
    private final HashSet<String> terms;
    private AbsoluteRange boundaries;

    public TermsHistogramResult(@Nullable DateHistogramAggregation result, String originalQuery, String builtQuery, long size, long tookMs, Searches.DateHistogramInterval interval, List<String> fields) {
        super(originalQuery, builtQuery, tookMs);
        this.size = size;
        this.interval = interval;
        this.result = Maps.newTreeMap();
        this.terms = new HashSet<>();

        if (result != null) {
            for (DateHistogramAggregation.DateHistogram histogram : result.getBuckets()) {
                final DateTime keyAsDate = new DateTime(histogram.getKey());
                final TermsAggregation termsAggregation = histogram.getFilterAggregation(Searches.AGG_FILTER).getTermsAggregation(Searches.AGG_TERMS);
                final MissingAggregation missingAgregation = histogram.getMissingAggregation("missing");
                final TermsResult termsResult = new TermsResult(termsAggregation, missingAgregation.getMissing(), histogram.getCount(), "", "", tookMs, fields);

                this.terms.addAll(termsResult.getTerms().keySet());
                this.result.put(keyAsDate.getMillis() / 1000L, termsResult);
            }
        }
    }

    public long getSize() {
        return size;
    }

    public Searches.DateHistogramInterval getInterval() {
        return interval;
    }

    public Map<Long, TermsResult> getResults() {
        return this.result;
    }

    public Set<String> getTerms() {
        return this.terms;
    }

    /*
     * Extract from and to fields from the built query to determine
     * histogram boundaries.
     */
    @Nullable
    public AbsoluteRange getHistogramBoundaries() {
        if (boundaries == null) {
            boundaries = Tools.extractHistogramBoundaries(getBuiltQuery()).orElse(null);
        }

        return boundaries;
    }

    public static TermsHistogramResult empty(String originalQuery, String builtQuery, long size, Searches.DateHistogramInterval interval) {
        return new TermsHistogramResult(null, originalQuery, builtQuery, size, 0L, interval, Collections.emptyList());
    }
}
