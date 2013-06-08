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

package org.graylog2.indexer.searches;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog2.Core;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.plugin.Tools;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryString;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Searches {

	private final Core server;
	private final Client c;
	
	public Searches(Client client, Core server) {
		this.server = server;
		this.c = client;
	}
	
	public SearchResult universalSearch(String query, int timerange) {
		SearchRequestBuilder srb = c.prepareSearch();
		srb.setIndices(server.getDeflector().getAllDeflectorIndexNames()); // XXX 020: have a method that builds time ranged index requests
		srb.setQuery(queryString(query));
		srb.setSize(150);
		srb.setFilter(IndexHelper.getTimestampRangeFilter(timerange));
		srb.addSort("timestamp", SortOrder.DESC);
		
		SearchResponse r = c.search(srb.request()).actionGet();
		return new SearchResult(r.getHits(), query, r.getTook());
	}
	
	public DateHistogramResult universalSearchHistogram(String query, Indexer.DateHistogramInterval interval, int timerange) {
		DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
				.field("timestamp")
				.facetFilter(IndexHelper.getTimestampRangeFilter(timerange))
				.interval(interval.toString().toLowerCase());
		
		SearchRequestBuilder srb = c.prepareSearch();
		srb.setIndices(server.getDeflector().getAllDeflectorIndexNames()); // XXX 020: have a method that builds time ranged index requests
		srb.setQuery(queryString(query));
		srb.addFacet(fb);
		
		SearchResponse r = c.search(srb.request()).actionGet();
		return new DateHistogramResult((DateHistogramFacet) r.getFacets().facet("histogram"), query, interval, r.getTook());
	}

    public SearchHit firstOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.DESC);
    }

    public SearchHit lastOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.ASC);
    }

    private SearchHit oneOfIndex(String index, QueryBuilder q, SortOrder sort) {
        SearchRequestBuilder srb = c.prepareSearch();
        srb.setIndices(index);
        srb.setQuery(q);
        srb.setSize(1);
        srb.addSort("timestamp", sort);

        SearchResponse r = c.search(srb.request()).actionGet();
        if (r.getHits() != null && r.getHits().totalHits() > 0) {
            return r.getHits().getAt(0);
        } else {
            return null;
        }
    }

	
}
