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

import com.google.common.collect.Maps;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.graylog2.indexer.searches.Searches;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DateHistogramResult extends HistogramResult {
	
	private final DateHistogramFacet result;
	private final Searches.DateHistogramInterval interval;

	public DateHistogramResult(DateHistogramFacet result, String originalQuery, BytesReference builtQuery, Searches.DateHistogramInterval interval, TimeValue took) {
        super(originalQuery, builtQuery, took);

		this.result = result;
		this.interval = interval;
	}

    @Override
	public Searches.DateHistogramInterval getInterval() {
		return interval;
	}

    @Override
	public Map<Long, Long> getResults() {
		Map<Long, Long> results = Maps.newTreeMap();
		
		for (DateHistogramFacet.Entry e : result) {
			results.put(e.getTime()/1000, e.getCount());
		}
		
		return results;
	}
	
}
