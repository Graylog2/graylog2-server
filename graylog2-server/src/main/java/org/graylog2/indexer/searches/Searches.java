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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.FacetPhaseExecutionException;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;
import org.elasticsearch.search.facet.statistical.StatisticalFacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog2.Core;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.*;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.Tools;

import javax.ws.rs.WebApplicationException;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryString;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Searches {

	private final Core server;
	private final Client c;

    private final static int LIMIT = 150;

    private final static String TERMS_FACET_NAME = "gl2_terms";
    private final static String STATS_FACET_NAME = "gl2_stats";

    public Searches(Client client, Core server) {
		this.server = server;
		this.c = client;
	}
	
	public SearchResult search(String query, TimeRange range, int limit, int offset) throws IndexHelper.InvalidRangeFormatException {
        if(limit <= 0) {
            limit = LIMIT;
        }

		SearchResponse r = c.search(standardSearchRequest(query, limit, offset, range, SortOrder.DESC).request()).actionGet();
		return new SearchResult(r.getHits(), query, r.getTook());
	}

    public TermsResult terms(String field, int size, String query, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        if (size == 0) {
            size = 50;
        }

        SearchRequestBuilder srb = standardSearchRequest(query);

        TermsFacetBuilder terms = new TermsFacetBuilder(TERMS_FACET_NAME);
        terms.facetFilter(IndexHelper.getTimestampRangeFilter(range));
        terms.global(false);
        terms.field(field);
        terms.size(size);

        srb.addFacet(terms);

        SearchResponse r = c.search(srb.request()).actionGet();

        return new TermsResult(
                (TermsFacet) r.getFacets().facet(TERMS_FACET_NAME),
                query,
                r.getTook()
        );
    }

    public FieldStatsResult fieldStats(String field, String query, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        SearchRequestBuilder srb = standardSearchRequest(query);

        StatisticalFacetBuilder stats = new StatisticalFacetBuilder(STATS_FACET_NAME);
        stats.global(false);
        stats.facetFilter(IndexHelper.getTimestampRangeFilter(range));
        stats.field(field);

        srb.addFacet(stats);

        SearchResponse r;
        try {
            r = c.search(srb.request()).actionGet();
        }  catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
            throw new FieldTypeException();
        }

        return new FieldStatsResult(
                (StatisticalFacet) r.getFacets().facet(STATS_FACET_NAME),
                query,
                r.getTook()
        );
    }
	
	public HistogramResult histogram(String query, Indexer.DateHistogramInterval interval, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
				.field("timestamp")
				.facetFilter(IndexHelper.getTimestampRangeFilter(range))
				.interval(interval.toString().toLowerCase());
		
		SearchRequestBuilder srb = c.prepareSearch();
		srb.setIndices(server.getDeflector().getAllDeflectorIndexNames()); // XXX 020: have a method that builds time ranged index requests
		srb.setQuery(queryString(query));
		srb.addFacet(fb);
		
		SearchResponse r = c.search(srb.request()).actionGet();
		return new DateHistogramResult((DateHistogramFacet) r.getFacets().facet("histogram"), query, interval, r.getTook());
	}

    public HistogramResult fieldHistogram(String query, String field, Indexer.DateHistogramInterval interval, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
                .keyField("timestamp")
                .valueField(field)
                .facetFilter(IndexHelper.getTimestampRangeFilter(range))
                .interval(interval.toString().toLowerCase());

        SearchRequestBuilder srb = c.prepareSearch();
        srb.setIndices(server.getDeflector().getAllDeflectorIndexNames()); // XXX 020: have a method that builds time ranged index requests
        srb.setQuery(queryString(query));
        srb.addFacet(fb);

        SearchResponse r;
        try {
            r = c.search(srb.request()).actionGet();
        }  catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
            throw new FieldTypeException();
        }

        return new FieldHistogramResult((DateHistogramFacet) r.getFacets().facet("histogram"), query, interval, r.getTook());
    }

    public SearchHit firstOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.DESC);
    }

    public SearchHit lastOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.ASC);
    }

    private SearchRequestBuilder standardSearchRequest(String query) throws IndexHelper.InvalidRangeFormatException {
        return standardSearchRequest(query, 0, 0, null, null);
    }

    private SearchRequestBuilder standardSearchRequest(String query, int limit, int offset, TimeRange range, SortOrder sort) throws IndexHelper.InvalidRangeFormatException {
        SearchRequestBuilder srb = c.prepareSearch();
        srb.setIndices(server.getDeflector().getAllDeflectorIndexNames()); // XXX 020: have a method that builds time ranged index requests
        srb.setQuery(queryString(query));
        srb.setFrom(offset);

        if (limit > 0) {
            srb.setSize(limit);
        }

        if (range != null) {
            srb.setFilter(IndexHelper.getTimestampRangeFilter(range));
        }

        if (sort != null) {
            srb.addSort("timestamp", sort);
        }

        return srb;
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

    public class FieldTypeException extends Exception {
    }
}
