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
package org.graylog2.indexer.searches;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;
import org.elasticsearch.search.facet.statistical.StatisticalFacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog2.Configuration;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.indexer.results.FieldHistogramResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.results.TermsStatsResult;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryString;

@Singleton
public class Searches {
    private static final Logger LOG = LoggerFactory.getLogger(Searches.class);

    public static enum TermsStatsOrder {
        TERM,
        REVERSE_TERM,
        COUNT,
        REVERSE_COUNT,
        TOTAL,
        REVERSE_TOTAL,
        MIN,
        REVERSE_MIN,
        MAX,
        REVERSE_MAX,
        MEAN,
        REVERSE_MEAN
    }

    public static enum DateHistogramInterval {
        YEAR(Period.years(1)),
        QUARTER(Period.months(3)),
        MONTH(Period.months(1)),
        WEEK(Period.weeks(1)),
        DAY(Period.days(1)),
        HOUR(Period.hours(1)),
        MINUTE(Period.minutes(1));

        private final Period period;

        DateHistogramInterval(Period period) {
            this.period = period;
        }

        public Period getPeriod() {
            return period;
        }
    }


    private final Configuration configuration;
    private final Deflector deflector;
    private final IndexRangeService indexRangeService;
    private final Client c;

    private final static String TERMS_FACET_NAME = "gl2_terms";
    private final static String STATS_FACET_NAME = "gl2_stats";
    private final static String TERMS_STATS_FACET_NAME = "gl2_termsstats";

    @Inject
    public Searches(Configuration configuration,
                    Deflector deflector,
                    IndexRangeService indexRangeService,
                    Node node) {
        this.configuration = configuration;
        this.deflector = deflector;
        this.indexRangeService = indexRangeService;
        this.c = node.client();
    }

    public CountResult count(String query, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return count(query, range, null);
    }

    public CountResult count(String query, TimeRange range, String filter) throws IndexHelper.InvalidRangeFormatException {
        Set<String> indices = IndexHelper.determineAffectedIndices(indexRangeService, deflector, range);

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
        final Set<String> indices = IndexHelper.determineAffectedIndices(indexRangeService, deflector, range);
        final SearchRequestBuilder srb = standardSearchRequest(query, indices, limit, offset, range, null, false);
        if (range != null && filter != null) {
            srb.setPostFilter(standardFilters(range, filter));
        }

        // only request the fields we asked for otherwise we can't figure out which fields will be in the result set
        // until we've scrolled through the entire set.
        // TODO: Check if we can get away without loading the _source field.
        // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-fields.html#search-request-fields
        // "For backwards compatibility, if the fields parameter specifies fields which are not stored , it will
        // load the _source and extract it from it. This functionality has been replaced by the source filtering
        // parameter." -- So we should look at the source filtering parameter once we switched to ES 1.x.
        srb.addFields(fields.toArray(new String[fields.size()]));
        srb.addField("_source"); // always request the _source field because otherwise we can't access non-stored values

        final SearchRequest request = srb.setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(1, TimeUnit.MINUTES))
                .setSize(500).request(); // TODO magic numbers
        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug("ElasticSearch scroll query: {}", XContentHelper.convertToJson(request.source(), false));
            } catch (IOException ignored) {
            }
        }
        final SearchResponse r = c.search(request).actionGet();

        return new ScrollResult(c, query, request.source(), r, fields);
    }

    public SearchResult search(String query, TimeRange range, int limit, int offset, Sorting sorting) throws IndexHelper.InvalidRangeFormatException {
        return search(query, null, range, limit, offset, sorting);
    }

    public SearchResult search(String query, String filter, TimeRange range, int limit, int offset, Sorting sorting) throws IndexHelper.InvalidRangeFormatException {
        final SearchesConfig searchesConfig = SearchesConfigBuilder.newConfig()
                .setQuery(query)
                .setFilter(filter)
                .setRange(range)
                .setLimit(limit)
                .setOffset(offset)
                .setSorting(sorting)
                .build();

        return search(searchesConfig);
    }

    public SearchResult search(SearchesConfig config) throws IndexHelper.InvalidRangeFormatException {
        Set<IndexRange> indices = IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, deflector, config.range());

        Set<String> indexNames = Sets.newHashSet();
        for (IndexRange index : indices) {
            indexNames.add(index.getIndexName());
        }

        SearchRequest request = searchRequest(config, indexNames).request();

        SearchResponse r = c.search(request).actionGet();
        return new SearchResult(r.getHits(), indices, config.query(), request.source(), r.getTook());
    }

    public TermsResult terms(String field, int size, String query, String filter, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        if (size == 0) {
            size = 50;
        }

        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range));
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range));
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

    public TermsStatsResult termsStats(String keyField, String valueField, TermsStatsOrder order, int size, String query, String filter, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        if (size == 0) {
            size = 50;
        }

        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range));
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range));
        }

        TermsStatsFacetBuilder stats = new TermsStatsFacetBuilder(TERMS_STATS_FACET_NAME);
        stats.global(false);
        stats.keyField(keyField);
        stats.valueField(valueField);
        stats.order(TermsStatsFacet.ComparatorType.fromString(order.toString().toLowerCase()));
        stats.size(size);

        stats.facetFilter(standardFilters(range, filter));

        srb.addFacet(stats);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();

        return new TermsStatsResult(
                (TermsStatsFacet) r.getFacets().facet(TERMS_STATS_FACET_NAME),
                query,
                request.source(),
                r.getTook()
        );
    }

    public TermsStatsResult termsStats(String keyField, String valueField, TermsStatsOrder order, int size, String query, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return termsStats(keyField, valueField, order, size, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        return fieldStats(field, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, String filter, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        SearchRequestBuilder srb;

        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range));
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range));
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
        } catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
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

    public HistogramResult histogram(String query, DateHistogramInterval interval, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        return histogram(query, interval, null, range);
    }

    public HistogramResult histogram(String query, DateHistogramInterval interval, String filter, TimeRange range) throws IndexHelper.InvalidRangeFormatException {
        DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
                .field("timestamp")
                .interval(interval.toString().toLowerCase());

        fb.facetFilter(standardFilters(range, filter));

        QueryStringQueryBuilder qs = queryString(query);
        qs.allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        SearchRequestBuilder srb = c.prepareSearch();
        final Set<String> affectedIndices = IndexHelper.determineAffectedIndices(indexRangeService, deflector, range);
        srb.setIndices(affectedIndices.toArray(new String[affectedIndices.size()]));
        srb.setQuery(qs);
        srb.addFacet(fb);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
        return new DateHistogramResult((DateHistogramFacet) r.getFacets().facet("histogram"), query,
                request.source(),
                interval, r.getTook());
    }

    public HistogramResult fieldHistogram(String query, String field, DateHistogramInterval interval, String filter, TimeRange range) throws FieldTypeException, IndexHelper.InvalidRangeFormatException {
        DateHistogramFacetBuilder fb = FacetBuilders.dateHistogramFacet("histogram")
                .keyField("timestamp")
                .valueField(field)
                .interval(interval.toString().toLowerCase());

        fb.facetFilter(standardFilters(range, filter));

        QueryStringQueryBuilder qs = queryString(query);
        qs.allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        SearchRequestBuilder srb = c.prepareSearch();
        final Set<String> affectedIndices = IndexHelper.determineAffectedIndices(indexRangeService, deflector, range);
        srb.setIndices(affectedIndices.toArray(new String[affectedIndices.size()]));
        srb.setQuery(qs);
        srb.addFacet(fb);

        SearchResponse r;
        final SearchRequest request = srb.request();
        try {
            r = c.search(request).actionGet();
        } catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
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

    private SearchRequestBuilder searchRequest(SearchesConfig config, Set<String> indices) throws IndexHelper.InvalidRangeFormatException {
        final SearchRequestBuilder request;

        if (config.filter() == null) {
            request = standardSearchRequest(config.query(), indices, config.limit(), config.offset(), config.range(), config.sorting());
        } else {
            request = filteredSearchRequest(config.query(), config.filter(), indices, config.limit(), config.offset(), config.range(), config.sorting());
        }

        if (config.fields() != null) {
            // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-fields.html#search-request-fields
            // "For backwards compatibility, if the fields parameter specifies fields which are not stored , it will
            // load the _source and extract it from it. This functionality has been replaced by the source filtering
            // parameter."
            // TODO: Look at the source filtering parameter once we switched to ES 1.x.
            request.addFields(config.fields().toArray(new String[config.fields().size()]));
        }

        return request;
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
        srb.setIndices(indices.toArray(new String[indices.size()]));

        if (query.trim().equals("*")) {
            srb.setQuery(matchAllQuery());
        } else {
            QueryStringQueryBuilder qs = queryString(query);
            qs.allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());
            srb.setQuery(qs);
        }

        srb.setFrom(offset);

        if (limit > 0) {
            srb.setSize(limit);
        }

        if (range != null) {
            srb.setPostFilter(IndexHelper.getTimestampRangeFilter(range));
        }

        if (sort != null) {
            srb.addSort(sort.getField(), sort.asElastic());
        }

        if (highlight && configuration.isAllowHighlighting()) {
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
            srb.setPostFilter(standardFilters(range, filter));
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

        if (filter != null && !filter.isEmpty() && !filter.equals("*")) {
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
