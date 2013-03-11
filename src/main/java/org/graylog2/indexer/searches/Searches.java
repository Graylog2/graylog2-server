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
import org.graylog2.Core;

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
	
	public SearchResult universalSearch(String query) {
		SearchRequestBuilder srb = c.prepareSearch();
		srb.setIndices(server.getDeflector().getAllDeflectorIndexNames()); // XXX 020: have a method that builds time ranged index requests
		srb.setQuery(queryString(query));

		SearchResponse r = c.search(srb.request()).actionGet();
		return new SearchResult(r.hits(), query, r.took());
	}
	
}
