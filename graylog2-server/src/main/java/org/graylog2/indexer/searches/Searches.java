/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.searches;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import org.elasticsearch.ElasticsearchException;
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
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStats;
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
import org.graylog2.indexer.searches.timeranges.TimeRanges;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

@Singleton
public class Searches {
    private static final Logger LOG = LoggerFactory.getLogger(Searches.class);

    public final static String AGG_TERMS = "gl2_terms";
    public final static String AGG_STATS = "gl2_stats";
    public final static String AGG_TERMS_STATS = "gl2_termsstats";
    public static final String AGG_FILTER = "gl2_filter";
    public static final String AGG_HISTOGRAM = "gl2_histogram";
    public static final String AGG_EXTENDED_STATS = "gl2_extended_stats";

    public enum TermsStatsOrder {
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

    public enum DateHistogramInterval {
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

        public DateHistogram.Interval toESInterval() {
            switch (this.name()) {
                case "MINUTE":
                    return DateHistogram.Interval.MINUTE;
                case "HOUR":
                    return DateHistogram.Interval.HOUR;
                case "DAY":
                    return DateHistogram.Interval.DAY;
                case "WEEK":
                    return DateHistogram.Interval.WEEK;
                case "MONTH":
                    return DateHistogram.Interval.MONTH;
                case "QUARTER":
                    return DateHistogram.Interval.QUARTER;
                default:
                    return DateHistogram.Interval.YEAR;
            }
        }
    }


    private final Configuration configuration;
    private final Deflector deflector;
    private final IndexRangeService indexRangeService;
    private final Client c;
    private final MetricRegistry metricRegistry;
    private final Timer esRequestTimer;
    private final Histogram esTimeRangeHistogram;

    @Inject
    public Searches(Configuration configuration,
                    Deflector deflector,
                    IndexRangeService indexRangeService,
                    Client client,
                    MetricRegistry metricRegistry) {
        this.configuration = checkNotNull(configuration);
        this.deflector = checkNotNull(deflector);
        this.indexRangeService = checkNotNull(indexRangeService);
        this.c = checkNotNull(client);
        this.metricRegistry = checkNotNull(metricRegistry);

        this.esRequestTimer = metricRegistry.timer(name(Searches.class, "elasticsearch", "requests"));
        this.esTimeRangeHistogram = metricRegistry.histogram(name(Searches.class, "elasticsearch", "ranges"));
    }

    public CountResult count(String query, TimeRange range) {
        return count(query, range, null);
    }

    public CountResult count(String query, TimeRange range, String filter) {
        Set<String> indices = IndexHelper.determineAffectedIndices(indexRangeService, deflector, range);

        SearchRequest request;
        if (filter == null) {
            request = standardSearchRequest(query, indices, range).request();
        } else {
            request = filteredSearchRequest(query, filter, indices, range).request();
        }
        request.searchType(SearchType.COUNT);

        SearchResponse r = c.search(request).actionGet();
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);
        return new CountResult(r.getHits().getTotalHits(), r.getTookInMillis(), r.getHits());
    }

    public ScrollResult scroll(String query, TimeRange range, int limit, int offset, List<String> fields, String filter) {
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
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        return new ScrollResult(c, query, request.source(), r, fields);
    }

    public SearchResult search(String query, TimeRange range, int limit, int offset, Sorting sorting) {
        return search(query, null, range, limit, offset, sorting);
    }

    public SearchResult search(String query, String filter, TimeRange range, int limit, int offset, Sorting sorting) {
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

    public SearchResult search(SearchesConfig config) {
        Set<IndexRange> indices = IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, deflector, config.range());

        Set<String> indexNames = Sets.newHashSet();
        for (IndexRange index : indices) {
            indexNames.add(index.indexName());
        }

        SearchRequest request = searchRequest(config, indexNames).request();

        SearchResponse r = c.search(request).actionGet();
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        return new SearchResult(r.getHits(), indices, config.query(), request.source(), r.getTook());
    }

    public TermsResult terms(String field, int size, String query, String filter, TimeRange range) {
        if (size == 0) {
            size = 50;
        }

        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range), range);
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range), range);
        }

        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .subAggregation(
                        AggregationBuilders.terms(AGG_TERMS)
                                .field(field)
                                .size(size))
                .subAggregation(
                        AggregationBuilders.missing("missing")
                                .field(field))
                .filter(standardFilters(range, filter));

        srb.addAggregation(builder);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new TermsResult(
                (Terms) f.getAggregations().get(AGG_TERMS),
                (Missing) f.getAggregations().get("missing"),
                f.getDocCount(),
                query,
                request.source(),
                r.getTook()
        );
    }

    public TermsResult terms(String field, int size, String query, TimeRange range) {
        return terms(field, size, query, null, range);
    }

    public TermsStatsResult termsStats(String keyField, String valueField, TermsStatsOrder order, int size, String query, String filter, TimeRange range) {
        if (size == 0) {
            size = 50;
        }

        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range), range);
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range), range);
        }


        Terms.Order termsOrder;
        switch (order) {
            case COUNT:
                termsOrder = Terms.Order.count(true);
                break;
            case REVERSE_COUNT:
                termsOrder = Terms.Order.count(false);
                break;
            case TERM:
                termsOrder = Terms.Order.term(true);
                break;
            case REVERSE_TERM:
                termsOrder = Terms.Order.term(false);
                break;
            case MIN:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "min", true);
                break;
            case REVERSE_MIN:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "min", false);
                break;
            case MAX:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "max", true);
                break;
            case REVERSE_MAX:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "max", false);
                break;
            case MEAN:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "avg", true);
                break;
            case REVERSE_MEAN:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "avg", false);
                break;
            case TOTAL:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "sum", true);
                break;
            case REVERSE_TOTAL:
                termsOrder = Terms.Order.aggregation(AGG_STATS, "sum", false);
                break;
            default:
                termsOrder = Terms.Order.count(true);
        }

        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .subAggregation(
                        AggregationBuilders.terms(AGG_TERMS_STATS)
                                .field(keyField)
                                .subAggregation(AggregationBuilders.stats(AGG_STATS).field(valueField))
                                .order(termsOrder)
                                .size(size))
                .filter(standardFilters(range, filter));

        srb.addAggregation(builder);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new TermsStatsResult(
                (Terms) f.getAggregations().get(AGG_TERMS_STATS),
                query,
                request.source(),
                r.getTook()
        );
    }

    public TermsStatsResult termsStats(String keyField, String valueField, TermsStatsOrder order, int size, String query, TimeRange range) {
        return termsStats(keyField, valueField, order, size, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, TimeRange range) throws FieldTypeException {
        return fieldStats(field, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, String filter, TimeRange range) throws FieldTypeException {
        SearchRequestBuilder srb;

        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range), range);
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, deflector, range), range);
        }

        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .filter(standardFilters(range, filter))
                .subAggregation(AggregationBuilders.extendedStats(AGG_EXTENDED_STATS).field(field));

        srb.addAggregation(builder);

        SearchResponse r;
        final SearchRequest request;
        try {
            request = srb.request();
            r = c.search(request).actionGet();
        } catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
            throw new FieldTypeException(e);
        }
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new FieldStatsResult(
                (ExtendedStats) f.getAggregations().get(AGG_EXTENDED_STATS),
                r.getHits(),
                query,
                request.source(),
                r.getTook()
        );
    }

    public HistogramResult histogram(String query, DateHistogramInterval interval, TimeRange range) {
        return histogram(query, interval, null, range);
    }

    public HistogramResult histogram(String query, DateHistogramInterval interval, String filter, TimeRange range) {
        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .subAggregation(
                        AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                                .field("timestamp")
                                .interval(interval.toESInterval()))
                .filter(standardFilters(range, filter));

        QueryStringQueryBuilder qs = queryStringQuery(query);
        qs.allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        SearchRequestBuilder srb = c.prepareSearch();
        final Set<String> affectedIndices = IndexHelper.determineAffectedIndices(indexRangeService, deflector, range);
        srb.setIndices(affectedIndices.toArray(new String[affectedIndices.size()]));
        srb.setQuery(qs);
        srb.addAggregation(builder);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new DateHistogramResult(
                (DateHistogram) f.getAggregations().get(AGG_HISTOGRAM),
                query,
                request.source(),
                interval,
                r.getTook());
    }

    public HistogramResult fieldHistogram(String query, String field, DateHistogramInterval interval, String filter, TimeRange range) throws FieldTypeException {
        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .subAggregation(
                        AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                                .field("timestamp")
                                .subAggregation(AggregationBuilders.stats(AGG_STATS).field(field))
                                .interval(interval.toESInterval())
                )
                .filter(standardFilters(range, filter));

        QueryStringQueryBuilder qs = queryStringQuery(query);
        qs.allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        SearchRequestBuilder srb = c.prepareSearch();
        final Set<String> affectedIndices = IndexHelper.determineAffectedIndices(indexRangeService, deflector, range);
        srb.setIndices(affectedIndices.toArray(new String[affectedIndices.size()]));
        srb.setQuery(qs);
        srb.addAggregation(builder);

        SearchResponse r;
        final SearchRequest request = srb.request();
        try {
            r = c.search(request).actionGet();
        } catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
            throw new FieldTypeException(e);
        }
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new FieldHistogramResult(
                (DateHistogram) f.getAggregations().get(AGG_HISTOGRAM),
                query,
                request.source(),
                interval,
                r.getTook());
    }

    /**
     * <em>WARNING:</em> The name of that method is wrong and it returns the <em>newest</em> message of an index!
     */
    public SearchHit firstOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.DESC);
    }

    /**
     * Find the oldest message timestamp in the given index.
     *
     * @param index Name of the index to query.
     * @return the oldest message timestamp in the given index, or {@code null} if it couldn't be found.
     */
    public DateTime findOldestMessageTimestampOfIndex(String index) {
        final FilterAggregationBuilder builder = AggregationBuilders.filter("agg")
                .filter(FilterBuilders.existsFilter("timestamp"))
                .subAggregation(AggregationBuilders.min("ts_min").field("timestamp"));
        final SearchRequestBuilder srb = c.prepareSearch()
                .setIndices(index)
                .setSearchType(SearchType.COUNT)
                .addAggregation(builder);

        final SearchResponse response;
        try {
            response = c.search(srb.request()).actionGet();
        } catch (IndexMissingException e) {
            throw e;
        } catch (ElasticsearchException e) {
            LOG.error("Error while calculating oldest timestamp in index <" + index + ">", e);
            return null;
        }
        esRequestTimer.update(response.getTookInMillis(), TimeUnit.MILLISECONDS);

        final Filter f = response.getAggregations().get("agg");
        if (f.getDocCount() == 0L) {
            LOG.debug("No documents with attribute \"timestamp\" found in index <{}>", index);
            return null;
        }

        final Min min = f.getAggregations().get("ts_min");
        final String minTimeStamp = min.getValueAsString();
        final DateTimeFormatter formatter = DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT).withZoneUTC();

        return formatter.parseDateTime(minTimeStamp);
    }

    /**
     * <em>WARNING:</em> The name of that method is wrong and it returns the <em>oldest</em> message of an index!
     */
    public SearchHit lastOfIndex(String index) {
        return oneOfIndex(index, matchAllQuery(), SortOrder.ASC);
    }

    /**
     * Find the newest message timestamp in the given index.
     *
     * @param index Name of the index to query.
     * @return the youngest message timestamp in the given index, or {@code null} if it couldn't be found.
     */
    public DateTime findNewestMessageTimestampOfIndex(String index) {
        final FilterAggregationBuilder builder = AggregationBuilders.filter("agg")
                .filter(FilterBuilders.existsFilter("timestamp"))
                .subAggregation(AggregationBuilders.max("ts_max").field("timestamp"));
        final SearchRequestBuilder srb = c.prepareSearch()
                .setIndices(index)
                .setSearchType(SearchType.COUNT)
                .addAggregation(builder);

        final SearchResponse response;
        try {
            response = c.search(srb.request()).actionGet();
        } catch (IndexMissingException e) {
            throw e;
        } catch (ElasticsearchException e) {
            LOG.error("Error while calculating newest timestamp in index <" + index + ">", e);
            return null;
        }
        esRequestTimer.update(response.getTookInMillis(), TimeUnit.MILLISECONDS);

        final Filter f = response.getAggregations().get("agg");
        if (f.getDocCount() == 0L) {
            LOG.debug("No documents with attribute \"timestamp\" found in index <{}>", index);
            return null;
        }

        final Max max = f.getAggregations().get("ts_max");
        final String maxTimeStamp = max.getValueAsString();
        final DateTimeFormatter formatter = DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT).withZoneUTC();

        return formatter.parseDateTime(maxTimeStamp);
    }

    private SearchRequestBuilder searchRequest(SearchesConfig config, Set<String> indices) {
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

    private SearchRequestBuilder standardSearchRequest(String query, Set<String> indices) {
        return standardSearchRequest(query, indices, 0, 0, null, null);
    }

    private SearchRequestBuilder standardSearchRequest(String query, Set<String> indices, TimeRange range) {
        return standardSearchRequest(query, indices, 0, 0, range, null);
    }

    private SearchRequestBuilder standardSearchRequest(String query,
                                                       Set<String> indices,
                                                       int limit,
                                                       int offset,
                                                       TimeRange range,
                                                       Sorting sort) {
        return standardSearchRequest(query, indices, limit, offset, range, sort, true);
    }

    private SearchRequestBuilder standardSearchRequest(
            String query,
            Set<String> indices,
            int limit,
            int offset,
            TimeRange range,
            Sorting sort,
            boolean highlight) {
        if (query == null || query.trim().isEmpty()) {
            query = "*";
        }

        SearchRequestBuilder srb = c.prepareSearch();
        srb.setIndices(indices.toArray(new String[indices.size()]));

        if (query.trim().equals("*")) {
            srb.setQuery(matchAllQuery());
        } else {
            QueryStringQueryBuilder qs = queryStringQuery(query);
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

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices) {
        return filteredSearchRequest(query, filter, indices, 0, 0, null, null);
    }

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices, TimeRange range) {
        return filteredSearchRequest(query, filter, indices, 0, 0, range, null);
    }

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices, int limit, int offset, TimeRange range, Sorting sort) {
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
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        if (r.getHits() != null && r.getHits().totalHits() > 0) {
            return r.getHits().getAt(0);
        } else {
            return null;
        }
    }

    private BoolFilterBuilder standardFilters(TimeRange range, String filter) {
        BoolFilterBuilder bfb = FilterBuilders.boolFilter();

        boolean set = false;

        if (range != null) {
            bfb.must(IndexHelper.getTimestampRangeFilter(range));
            set = true;
            esTimeRangeHistogram.update(TimeRanges.toSeconds(range));
        }

        if (filter != null && !filter.isEmpty() && !filter.equals("*")) {
            bfb.must(FilterBuilders.queryFilter(QueryBuilders.queryStringQuery(filter)));
            set = true;
        }

        if (!set) {
            throw new RuntimeException("Either range or filter must be set.");
        }

        return bfb;
    }

    public static class FieldTypeException extends Exception {
        public FieldTypeException(Throwable e) {
            super(e);
        }
    }
}