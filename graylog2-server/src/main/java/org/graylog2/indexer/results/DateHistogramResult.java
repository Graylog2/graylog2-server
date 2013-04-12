/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.graylog2.indexer.Indexer;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DateHistogramResult {
	
	private final String originalQuery;
	private final DateHistogramFacet result;
	private final Indexer.DateHistogramInterval interval;
	private final TimeValue took;

	public DateHistogramResult(DateHistogramFacet result, String originalQuery, Indexer.DateHistogramInterval interval, TimeValue took) {
		this.originalQuery = originalQuery;
		this.result = result;
		this.interval = interval;
		this.took = took;
	}
	
	public String getOriginalQuery() {
		 return originalQuery;
	}
	
	public TimeValue took() {
		return took;
	}
	
	public Indexer.DateHistogramInterval getInterval() {
		return interval;
	}
	
	public Map<Long, Long> getResults() {
		Map<Long, Long> results = Maps.newTreeMap();
		
		for (DateHistogramFacet.Entry e : result) {
			results.put(e.getTime()/1000, e.getCount());
		}
		
		return results;
	}
	
}
