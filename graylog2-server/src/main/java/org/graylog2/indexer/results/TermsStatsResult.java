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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.searchbox.core.search.aggregation.StatsAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.graylog2.indexer.searches.Searches;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TermsStatsResult extends IndexQueryResult {
    private static final Comparator<Map<String, Object>> COMPARATOR = new Comparator<Map<String, Object>>() {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            double o1Mean = (double) o1.get("mean");
            double o2Mean = (double) o2.get("mean");
            if (o1Mean > o2Mean) {
                return -1;
            } else if (o1Mean < o2Mean) {
                return 1;
            }
            return 0;
        }
    };
    private final List<Map<String, Object>> terms;

    public TermsStatsResult(Terms facet, String originalQuery, BytesReference builtQuery, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.terms = facet.getBuckets().stream()
            .map(e -> {
                final Map<String, Object> resultMap = Maps.newHashMap();

                resultMap.put("key_field", e.getKey());

                resultMap.put("count", e.getDocCount());

                final Stats stats = e.getAggregations().get(Searches.AGG_STATS);
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

    public TermsStatsResult(TermsAggregation terms, String originalQuery, BytesReference builtQuery, TimeValue took) {
        super(originalQuery, builtQuery, took);

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

    public List<Map<String, Object>> getResults() {
        return this.terms;
    }
}
