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

package org.graylog2.indexer.counts;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.graylog2.Core;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.plugin.Tools;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Counts {

	private final Core server;
	private final Client c;
	
	public Counts(Client client, Core server) {
		this.server = server;
		this.c = client;
	}
	
	public DateHistogramResult totalCount(Indexer.DateHistogramInterval interval, int timerange) {
		String from = Tools.buildElasticSearchTimeFormat(Tools.getUTCTimestamp()-timerange);
		FilterBuilder timestampFilter = FilterBuilders.rangeFilter("timestamp").from(from);
		
		DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
				.field("timestamp")
				.facetFilter(timestampFilter)
				.interval(interval.toString().toLowerCase());
		
		SearchRequestBuilder srb = c.prepareSearch();
		srb.setIndices(server.getDeflector().getAllDeflectorIndexNames()); // XXX 020: have a method that builds time ranged index requests
		srb.setQuery(matchAllQuery());
		srb.addFacet(fb);
		
		SearchResponse r = c.search(srb.request()).actionGet();
		return new DateHistogramResult((DateHistogramFacet) r.facets().facet("histogram"), "match_all", interval, r.took());
	}
	
}
