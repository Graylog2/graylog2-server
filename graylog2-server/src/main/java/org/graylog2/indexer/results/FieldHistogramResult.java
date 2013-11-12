/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.indexer.results;

import com.google.common.collect.Maps;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.graylog2.indexer.Indexer;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldHistogramResult extends HistogramResult {

    private final DateHistogramFacet result;
    private final Indexer.DateHistogramInterval interval;

    public FieldHistogramResult(DateHistogramFacet result, String originalQuery, BytesReference builtQuery, Indexer.DateHistogramInterval interval, TimeValue took) {
        super(originalQuery, builtQuery, took);

        this.result = result;
        this.interval = interval;
    }

    public Indexer.DateHistogramInterval getInterval() {
        return interval;
    }

    public Map<Long, Map<String, Object>> getResults() {
        Map<Long, Map<String, Object>> results = Maps.newTreeMap();

        for (DateHistogramFacet.Entry e : result) {
            Map<String, Object> resultMap = Maps.newHashMap();

            resultMap.put("count", e.getCount());
            resultMap.put("min", e.getMin());
            resultMap.put("max", e.getMax());
            resultMap.put("total", e.getTotal());
            resultMap.put("total_count", e.getTotalCount());
            resultMap.put("mean", e.getMean());

            results.put(e.getTime()/1000, resultMap);
        }

        return results;
    }

}
