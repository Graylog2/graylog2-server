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
import io.searchbox.core.search.aggregation.StatsAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.graylog2.indexer.searches.Searches;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TermsStatsResult extends IndexQueryResult {
    private static final Comparator<Map<String, Object>> COMPARATOR = (o1, o2) -> {
        double o1Mean = (double) o1.get("mean");
        double o2Mean = (double) o2.get("mean");
        if (o1Mean > o2Mean) {
            return -1;
        } else if (o1Mean < o2Mean) {
            return 1;
        }
        return 0;
    };
    private final List<Map<String, Object>> terms;

    public TermsStatsResult(TermsAggregation terms, String originalQuery, String builtQuery, long tookMs) {
        super(originalQuery, builtQuery, tookMs);

        this.terms = terms.getBuckets().stream()
            .map(e -> {
                final Map<String, Object> resultMap = Maps.newHashMap();

                resultMap.put("key_field", e.getKey());

                resultMap.put("count", e.getCount());

                final StatsAggregation stats = e.getStatsAggregation(Searches.AGG_STATS);
                resultMap.put("min", stats.getMin());
                resultMap.put("max", stats.getMax());
                resultMap.put("total", stats.getSum());
                resultMap.put("total_count", stats.getCount());
                resultMap.put("mean", stats.getAvg());

                return resultMap;
            })
            .sorted(COMPARATOR)
            .collect(Collectors.toList());
    }

    private TermsStatsResult(String originalQuery, String builtQuery) {
        super(originalQuery, builtQuery, 0);

        this.terms = Collections.emptyList();
    }

    public static TermsStatsResult empty(String originalQuery, String builtQuery) {
        return new TermsStatsResult(originalQuery, builtQuery);
    }

    public List<Map<String, Object>> getResults() {
        return this.terms;
    }
}
