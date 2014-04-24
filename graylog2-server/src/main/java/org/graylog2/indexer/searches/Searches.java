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
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilders;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryString;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Searches {
    private static final Logger log = LoggerFactory.getLogger(Searches.class);

    private final Core server;
	private final Client c;

    private final static int LIMIT = 150;

    private final static String TERMS_FACET_NAME = "gl2_terms";
    private final static String STATS_FACET_NAME = "gl2_stats";

    public Searches(Client client, Core server) {
		this.server = server;
		this.c = client;
	}

    public CountResult count(String query, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return count(query, range, null);
    }

    public CountResult count(String query, TimeRange range, String filter) throws IndexHelper.InvalidRangeFormatException {
        Set<String> indices = IndexHelper.determineAffectedIndices(server, range);

        SearchRequest request;
        if (filter == null) {
            request = standardSearchRequest(query, indices, range).request();
        } else {
            request = filteredSearchRequest(query, filter, indices, range).request();
        }
        request.searchType(SearchType.COUNT);

        SearchResponse r = c.search(request).actionGet();
        return new CountResult(r.getHits().getTotalHits(), r.getTookInMillis(), r.getHits());
    }

    public ScrollResult scroll(String query, TimeRange range, int limit, int offset, List<String> fields, String filter) throws IndexHelper.InvalidRangeFormatException {
        final Set<String> indices = IndexHelper.determineAffectedIndices(server, range);
        final SearchRequestBuilder srb = standardSearchRequest(query, indices, limit, offset, range, null, false);
        if (range != null && filter != null) {
            srb.setPostFilter(standardFilters(range, filter));
        }

        // only request the fields we asked for otherwise we can't figure out which fields will be in the result set
        // until we've scrolled through the entire set.
        srb.addFields(fields.toArray(new String[fields.size()]));
        srb.addField("_source"); // always request the _source field because otherwise we can't access non-stored values

        final SearchRequest request = srb.setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(1, TimeUnit.MINUTES))
                .setSize(500).request(); // TODO magic numbers
        if (log.isDebugEnabled()) {
            try {
                log.debug("ElasticSearch scroll query: {}", XContentHelper.convertToJson(request.source(), false));
            } catch (IOException ignored) {}
        }
        final SearchResponse r = c.search(request).actionGet();

        return new ScrollResult(c, query, request.source(), r, fields);
    }

    public SearchResult search(String query, TimeRange range, int limit, int offset, Sorting sorting) throws IndexHelper.InvalidRangeFormatException {
        return search(query, null, range, limit, offset, sorting);
	}

    public SearchResult search(String query, String filter, TimeRange range, int limit, int offset, Sorting sorting) throws IndexHelper.InvalidRangeFormatException {
        if(limit <= 0) {
            limit = LIMIT;
        }

        Set<String> indices = IndexHelper.determineAffectedIndices(server, range);

        SearchRequest request;

        if (filter == null) {
            request = standardSearchRequest(query,
                                            indices,
                                            limit,
                                            offset,
                                            range,
                                            sorting).request();
        } else {
            request = filteredSearchRequest(query, filter, indices, limit, offset, range, sorting).request();
        }

        SearchResponse r = c.search(request).actionGet();
        return new SearchResult(r.getHits(), indices, query, request.source(), r.getTook());
    }

    public TermsResult terms(String field, int size, String query, String filter, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        if (size == 0) {
            size = 50;
        }

        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(server, range));
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(server, range));
        }

        TermsFacetBuilder terms = new TermsFacetBuilder(TERMS_FACET_NAME);
        terms.global(false);
        terms.field(field);
        terms.size(size);

        terms.facetFilter(standardFilters(range, filter));

        srb.addFacet(terms);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();

        return new TermsResult(
                (TermsFacet) r.getFacets().facet(TERMS_FACET_NAME),
                query,
                request.source(),
                r.getTook()
        );
    }

    public TermsResult terms(String field, int size, String query, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return terms(field, size, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        return fieldStats(field, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, String filter, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        SearchRequestBuilder srb;

        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(server, range));
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(server, range));
        }

        StatisticalFacetBuilder stats = new StatisticalFacetBuilder(STATS_FACET_NAME);
        stats.global(false);

        stats.facetFilter(standardFilters(range, filter));

        stats.field(field);

        srb.addFacet(stats);

        SearchResponse r;
        final SearchRequest request;
        try {
            request = srb.request();
            r = c.search(request).actionGet();
        }  catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
            throw new FieldTypeException(e);
        }

        return new FieldStatsResult(
                (StatisticalFacet) r.getFacets().facet(STATS_FACET_NAME),
                r.getHits(),
                query,
                request.source(),
                r.getTook()
        );
    }

    public HistogramResult histogram(String query, Indexer.DateHistogramInterval interval, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return histogram(query, interval, null, range);
    }

    public HistogramResult histogram(String query, Indexer.DateHistogramInterval interval, String filter, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
				.field("timestamp")
				.interval(interval.toString().toLowerCase());

        fb.facetFilter(standardFilters(range, filter));

        QueryStringQueryBuilder qs = queryString(query);
        qs.allowLeadingWildcard(server.getConfiguration().isAllowLeadingWildcardSearches());

        SearchRequestBuilder srb = c.prepareSearch();
		srb.setIndices(IndexHelper.determineAffectedIndices(server, range).toArray(new String[]{}));
		srb.setQuery(qs);
		srb.addFacet(fb);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
		return new DateHistogramResult((DateHistogramFacet) r.getFacets().facet("histogram"), query,
                                       request.source(),
                                       interval, r.getTook());
	}

    public HistogramResult fieldHistogram(String query, String field, Indexer.DateHistogramInterval interval, String filter, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
                .keyField("timestamp")
                .valueField(field)
                .interval(interval.toString().toLowerCase());

        fb.facetFilter(standardFilters(range, filter));

        QueryStringQueryBuilder qs = queryString(query);
        qs.allowLeadingWildcard(server.getConfiguration().isAllowLeadingWildcardSearches());

        SearchRequestBuilder srb = c.prepareSearch();
        srb.setIndices(IndexHelper.determineAffectedIndices(server, range).toArray(new String[]{}));
        srb.setQuery(qs);
        srb.addFacet(fb);

        SearchResponse r;
        final SearchRequest request = srb.request();
        try {
            r = c.search(request).actionGet();
        }  catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
            throw new FieldTypeException(e);
        }

        return new FieldHistogramResult((DateHistogramFacet) r.getFacets().facet("histogram"), query, request.source(),
                                        interval, r.getTook());
    }

    public SearchHit firstOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.DESC);
    }

    public SearchHit lastOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.ASC);
    }

    private SearchRequestBuilder standardSearchRequest(String query, Set<String> indices) throws IndexHelper.InvalidRangeFormatException {
        return standardSearchRequest(query, indices, 0, 0, null, null);
    }

    private SearchRequestBuilder standardSearchRequest(String query, Set<String> indices, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return standardSearchRequest(query, indices, 0, 0, range, null);
    }

    private SearchRequestBuilder standardSearchRequest(String query,
                                                       Set<String> indices,
                                                       int limit,
                                                       int offset,
                                                       TimeRange range,
                                                       Sorting sort) throws IndexHelper.InvalidRangeFormatException {
        return standardSearchRequest(query, indices, limit, offset, range, sort, true);
    }

    private SearchRequestBuilder standardSearchRequest(
            String query,
            Set<String> indices,
            int limit,
            int offset,
            TimeRange range,
            Sorting sort,
            boolean highlight) throws IndexHelper.InvalidRangeFormatException {
        if (query == null || query.trim().isEmpty()) {
            query = "*";
        }

        SearchRequestBuilder srb = c.prepareSearch();
        srb.setIndices(indices.toArray(new String[]{}));

        if (query.trim().equals("*")) {
            srb.setQuery(matchAllQuery());
        } else {
            QueryStringQueryBuilder qs = queryString(query);
            qs.allowLeadingWildcard(server.getConfiguration().isAllowLeadingWildcardSearches());
            srb.setQuery(qs);
        }

        srb.setFrom(offset);

        if (limit > 0) {
            srb.setSize(limit);
        }

        if (range != null) {
            srb.setFilter(IndexHelper.getTimestampRangeFilter(range));
        }

        if (sort != null) {
            srb.addSort(sort.getField(), sort.asElastic());
        }

        if (highlight && server.getConfiguration().isAllowHighlighting()) {
            srb.setHighlighterRequireFieldMatch(false);
            srb.addHighlightedField("*", 0, 0);
        }

        return srb;
    }

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices) throws IndexHelper.InvalidRangeFormatException {
        return filteredSearchRequest(query, filter, indices, 0, 0, null, null);
    }

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return filteredSearchRequest(query, filter, indices, 0, 0, range, null);
    }

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices, int limit, int offset, TimeRange range, Sorting sort) throws IndexHelper.InvalidRangeFormatException {
        SearchRequestBuilder srb = standardSearchRequest(query, indices, limit, offset, range, sort);

        if (range != null && filter != null) {
            srb.setFilter(standardFilters(range, filter));
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

    private BoolFilterBuilder standardFilters(TimeRange range, String filter) throws IndexHelper.InvalidRangeFormatException {
        BoolFilterBuilder bfb = FilterBuilders.boolFilter();

        boolean set = false;

        if (range != null) {
            bfb.must(IndexHelper.getTimestampRangeFilter(range));
            set = true;
        }

        if(filter != null && !filter.isEmpty() && !filter.equals("*")) {
            bfb.must(FilterBuilders.queryFilter(QueryBuilders.queryString(filter)));
            set = true;
        }

        if (!set) {
            throw new RuntimeException("Either range or filter must be set.");
        }

        return bfb;
    }

    public class FieldTypeException extends Exception {

        public FieldTypeException(Throwable e) {
            super(e);
        }

    }
}
