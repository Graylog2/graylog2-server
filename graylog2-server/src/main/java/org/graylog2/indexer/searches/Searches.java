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

import com.google.common.collect.Sets;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortParseElement;
import org.graylog2.Configuration;
import org.graylog2.database.NotFoundException;
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
import org.graylog2.indexer.searches.timeranges.TimeRanges;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import joptsimple.internal.Strings;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
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
    public static final String AGG_CARDINALITY = "gl2_field_cardinality";
    public static final String AGG_VALUE_COUNT = "gl2_value_count";
    private static final Pattern filterStreamIdPattern = Pattern.compile("^(.+[^\\p{Alnum}])?streams:([\\p{XDigit}]+)");


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

        public org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval toESInterval() {
            switch (this.name()) {
                case "MINUTE":
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.MINUTE;
                case "HOUR":
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.HOUR;
                case "DAY":
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.DAY;
                case "WEEK":
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.WEEK;
                case "MONTH":
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.MONTH;
                case "QUARTER":
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.QUARTER;
                default:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.YEAR;
            }
        }
    }


    private final Configuration configuration;
    private final IndexRangeService indexRangeService;
    private final Client c;
    private final Timer esRequestTimer;
    private final Histogram esTimeRangeHistogram;
    private final StreamService streamService;

    @Inject
    public Searches(Configuration configuration,
                    IndexRangeService indexRangeService,
                    Client client,
                    MetricRegistry metricRegistry,
                    StreamService streamService) {
        this.configuration = checkNotNull(configuration);
        this.indexRangeService = checkNotNull(indexRangeService);
        this.c = checkNotNull(client);

        this.esRequestTimer = metricRegistry.timer(name(Searches.class, "elasticsearch", "requests"));
        this.esTimeRangeHistogram = metricRegistry.histogram(name(Searches.class, "elasticsearch", "ranges"));
        this.streamService = streamService;
    }

    public CountResult count(String query, TimeRange range) {
        return count(query, range, null);
    }

    public CountResult count(String query, TimeRange range, String filter) {
        String indexPrefix = getIndexPrefixForFilter(filter);
        Set<String> indices = IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix);

        final SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, indices, range);
        } else {
            srb = filteredSearchRequest(query, filter, indices, range);
        }
        srb.setSize(0);

        final SearchResponse r = c.search(srb.request()).actionGet();
        recordEsMetrics(r, range);
        return CountResult.create(r.getHits().getTotalHits(), r.getTookInMillis());
    }

    public ScrollResult scroll(String query, TimeRange range, int limit, int offset, List<String> fields, String filter) {
        String indexPrefix = getIndexPrefixForFilter(filter);
        final Set<String> indices = IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix);

        // only request the fields we asked for otherwise we can't figure out which fields will be in the result set
        // until we've scrolled through the entire set.
        // TODO: Check if we can get away without loading the _source field.
        // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-fields.html#search-request-fields
        // "For backwards compatibility, if the fields parameter specifies fields which are not stored , it will
        // load the _source and extract it from it. This functionality has been replaced by the source filtering
        // parameter." -- So we should look at the source filtering parameter once we switched to ES 1.x.
        final SearchRequest request = standardSearchRequest(query, indices, limit, offset, range, filter, null, false)
                .setScroll(new TimeValue(1, TimeUnit.MINUTES))
                .setSize(500) // TODO magic numbers
                .addSort(SortBuilders.fieldSort(SortParseElement.DOC_FIELD_NAME))
                .addFields(fields.toArray(new String[fields.size()]))
                .addField("_source") // always request the _source field because otherwise we can't access non-stored values
                .request();

        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug("ElasticSearch scroll query: {}", XContentHelper.convertToJson(request.source(), false));
            } catch (IOException ignored) {
            }
        }
        final SearchResponse r = c.search(request).actionGet();
        recordEsMetrics(r, range);

        return new ScrollResult(c, query, request.source(), r, fields);
    }

    public SearchResult search(String query, TimeRange range, int limit, int offset, Sorting sorting) {
        return search(query, null, range, limit, offset, sorting);
    }

    public SearchResult search(String query, String filter, TimeRange range, int limit, int offset, Sorting sorting) {
        final SearchesConfig searchesConfig = SearchesConfig.builder()
                .query(query)
                .filter(filter)
                .range(range)
                .limit(limit)
                .offset(offset)
                .sorting(sorting)
                .build();

        return search(searchesConfig);
    }

    public SearchResult search(SearchesConfig config) {
        String indexPrefix = getIndexPrefixForFilter(config.filter());
        Set<IndexRange> indices = IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, config.range(), indexPrefix);

        Set<String> indexNames = Sets.newHashSet();
        for (IndexRange index : indices) {
            indexNames.add(index.indexName());
        }

        SearchRequest request = searchRequest(config, indexNames).request();

        SearchResponse r = c.search(request).actionGet();
        recordEsMetrics(r, config.range());

        return new SearchResult(r.getHits(), indices, config.query(), request.source(), r.getTook());
    }

    @Nullable
    private String getIndexPrefixForFilter(String filter) {
        final Optional<String> streamId = extractStreamId(filter);
        String indexPrefix = null;
        if (streamId.isPresent()) {
            try {
                final Stream stream = streamService.load(streamId.get());
                indexPrefix = stream.getIndexSet().getIndexPrefix();
            } catch (NotFoundException ignored) {
            }
        }
        return indexPrefix;
    }

    public TermsResult terms(String field, int size, String query, String filter, TimeRange range) {
        if (size == 0) {
            size = 50;
        }

        String indexPrefix = getIndexPrefixForFilter(filter);
        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix), range);
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix), range);
        }

        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .subAggregation(
                        AggregationBuilders.terms(AGG_TERMS)
                                .field(field)
                                .size(size))
                .subAggregation(
                        AggregationBuilders.missing("missing")
                                .field(field))
                .filter(standardAggregationFilters(range, filter));

        srb.addAggregation(builder);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
        recordEsMetrics(r, range);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new TermsResult(
                f.getAggregations().get(AGG_TERMS),
                f.getAggregations().get("missing"),
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

        String indexPrefix = getIndexPrefixForFilter(filter);
        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix), range);
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix), range);
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
                .filter(standardAggregationFilters(range, filter));

        srb.addAggregation(builder);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
        recordEsMetrics(r, range);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new TermsStatsResult(
                f.getAggregations().get(AGG_TERMS_STATS),
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
        // by default include the cardinality aggregation, as well.
        return fieldStats(field, query, filter, range, true, true, true);
    }

    public FieldStatsResult fieldStats(String field,
                                       String query,
                                       String filter,
                                       TimeRange range,
                                       boolean includeCardinality,
                                       boolean includeStats,
                                       boolean includeCount)
            throws FieldTypeException {
        String indexPrefix = getIndexPrefixForFilter(filter);
        SearchRequestBuilder srb;
        if (filter == null) {
            srb = standardSearchRequest(query, IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix), range);
        } else {
            srb = filteredSearchRequest(query, filter, IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix), range);
        }

        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .filter(standardAggregationFilters(range, filter));
        if (includeCount) {
            builder.subAggregation(AggregationBuilders.count(AGG_VALUE_COUNT).field(field));
        }
        if (includeStats) {
            builder.subAggregation(AggregationBuilders.extendedStats(AGG_EXTENDED_STATS).field(field));
        }
        if (includeCardinality) {
            builder.subAggregation(AggregationBuilders.cardinality(AGG_CARDINALITY).field(field));
        }

        srb.addAggregation(builder);

        SearchResponse r;
        final SearchRequest request;
        try {
            request = srb.request();
            r = c.search(request).actionGet();
        } catch (org.elasticsearch.action.search.SearchPhaseExecutionException e) {
            throw new FieldTypeException(e);
        }
        recordEsMetrics(r, range);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new FieldStatsResult(
                f.getAggregations().get(AGG_VALUE_COUNT),
                f.getAggregations().get(AGG_EXTENDED_STATS),
                f.getAggregations().get(AGG_CARDINALITY),
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
                .filter(standardAggregationFilters(range, filter));

        QueryStringQueryBuilder qs = queryStringQuery(query);
        qs.allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        String indexPrefix = getIndexPrefixForFilter(filter);
        final Set<String> affectedIndices = IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix);
        final SearchRequestBuilder srb = c.prepareSearch(affectedIndices.toArray(new String[affectedIndices.size()]))
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setQuery(qs)
                .addAggregation(builder);

        final SearchRequest request = srb.request();
        SearchResponse r = c.search(request).actionGet();
        recordEsMetrics(r, range);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new DateHistogramResult(
                f.getAggregations().get(AGG_HISTOGRAM),
                query,
                request.source(),
                interval,
                r.getTook());
    }

    public HistogramResult fieldHistogram(String query,
                                          String field,
                                          DateHistogramInterval interval,
                                          String filter,
                                          TimeRange range,
                                          boolean includeCardinality) throws FieldTypeException {
        final DateHistogramBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                .field("timestamp")
                .subAggregation(AggregationBuilders.stats(AGG_STATS).field(field))
                .interval(interval.toESInterval());

        if (includeCardinality) {
            dateHistogramBuilder.subAggregation(AggregationBuilders.cardinality(AGG_CARDINALITY).field(field));
        }

        FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER)
                .subAggregation(dateHistogramBuilder)
                .filter(standardAggregationFilters(range, filter));

        QueryStringQueryBuilder qs = queryStringQuery(query);
        qs.allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        String indexPrefix = getIndexPrefixForFilter(filter);
        SearchRequestBuilder srb = c.prepareSearch();
        final Set<String> affectedIndices = IndexHelper.determineAffectedIndices(indexRangeService, range, indexPrefix);
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
        recordEsMetrics(r, range);

        final Filter f = r.getAggregations().get(AGG_FILTER);
        return new FieldHistogramResult(
                f.getAggregations().get(AGG_HISTOGRAM),
                query,
                request.source(),
                interval,
                r.getTook());
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
        return standardSearchRequest(query, indices, limit, offset, range, null, sort, highlight);
    }

    private SearchRequestBuilder standardSearchRequest(
            String query,
            Set<String> indices,
            int limit,
            int offset,
            TimeRange range,
            String filter,
            Sorting sort,
            boolean highlight) {
        if (query == null || query.trim().isEmpty()) {
            query = "*";
        }

        final QueryBuilder queryBuilder;
        if (query.trim().equals("*")) {
            queryBuilder = matchAllQuery();
        } else {
            queryBuilder = queryStringQuery(query).allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());
        }

        final SearchRequestBuilder srb = c.prepareSearch(indices.toArray(new String[indices.size()]))
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setQuery(QueryBuilders.boolQuery().must(queryBuilder).filter(standardFilters(range, filter)))
                .setFrom(offset);

        if (limit > 0) {
            srb.setSize(limit);
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

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices, TimeRange range) {
        return filteredSearchRequest(query, filter, indices, 0, 0, range, null);
    }

    private SearchRequestBuilder filteredSearchRequest(String query, String filter, Set<String> indices, int limit, int offset, TimeRange range, Sorting sort) {
        return standardSearchRequest(query, indices, limit, offset, range, filter, sort, true);
    }

    private void recordEsMetrics(SearchResponse r, @Nullable TimeRange range) {
        esRequestTimer.update(r.getTookInMillis(), TimeUnit.MILLISECONDS);

        if (range != null) {
            esTimeRangeHistogram.update(TimeRanges.toSeconds(range));
        }
    }

    @Nullable
    private QueryBuilder standardFilters(TimeRange range, String filter) {
        BoolQueryBuilder bfb = null;

        if (range != null) {
            bfb = QueryBuilders.boolQuery();
            bfb.must(IndexHelper.getTimestampRangeFilter(range));
        }

        // Not creating a filter for a "*" value because an empty filter used to be submitted that way.
        if (!isNullOrEmpty(filter) && !filter.equals("*")) {
            if (bfb == null) {
                bfb = QueryBuilders.boolQuery();
            }
            bfb.must(QueryBuilders.queryStringQuery(filter));
        }

        return bfb;
    }

    private QueryBuilder standardAggregationFilters(TimeRange range, String filter) {
        final QueryBuilder filterBuilder = standardFilters(range, filter);

        // Throw an exception here to avoid exposing an internal Elasticsearch exception later.
        if (filterBuilder == null) {
            throw new RuntimeException("Either range or filter must be set.");
        }

        return filterBuilder;
    }

    public static class FieldTypeException extends Exception {
        public FieldTypeException(Throwable e) {
            super(e);
        }
    }


    /**
     * Extracts the last stream id from the filter string passed as part of the elasticsearch query. This is used later
     * to pass to possibly existing message decorators for stream-specific configurations.
     *
     * The assumption is that usually (when listing/searching messages for a stream) only a single stream filter is passed.
     * When this is not the case, only the last stream id will be taked into account.
     *
     * This is currently a workaround. A better solution would be to pass the stream id which is supposed to be the scope
     * for a search query as a separate parameter.
     *
     * @param filter
     * @return the optional stream id
     */
    public static Optional<String> extractStreamId(String filter) {
        if (Strings.isNullOrEmpty(filter)) {
            return Optional.empty();
        }
        final Matcher streamIdMatcher = filterStreamIdPattern.matcher(filter);
        if (streamIdMatcher.find()) {
            return Optional.of(streamIdMatcher.group(2));
        }
        return Optional.empty();
    }
}
