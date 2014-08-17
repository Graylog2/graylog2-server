/**
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
package org.graylog2.indexer.results;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class TermsStatsResult extends IndexQueryResult {

    private final TermsStatsFacet facet;

    public TermsStatsResult(TermsStatsFacet facet, String originalQuery, BytesReference builtQuery, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.facet = facet;
    }

    public List<Map<String, Object>> getResults() {
        List<Map<String, Object>> results = Lists.newArrayList();

        for (TermsStatsFacet.Entry e : facet.getEntries()) {
            Map<String, Object> resultMap = Maps.newHashMap();

            resultMap.put("key_field", e.getTerm().toString());

            resultMap.put("count", e.getCount());
            resultMap.put("min", e.getMin());
            resultMap.put("max", e.getMax());
            resultMap.put("total", e.getTotal());
            resultMap.put("total_count", e.getTotalCount());
            resultMap.put("mean", e.getMean());

            results.add(resultMap);
        }

        // Sort results by descending mean value
        Collections.sort(results, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                double o1Mean = (double)o1.get("mean");
                double o2Mean = (double)o2.get("mean");
                if (o1Mean > o2Mean) {
                    return -1;
                } else if (o1Mean < o2Mean) {
                    return 1;
                }
                return 0;
            }
        });

        return results;
    }

}
