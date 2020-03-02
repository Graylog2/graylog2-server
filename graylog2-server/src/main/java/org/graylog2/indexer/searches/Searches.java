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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import io.searchbox.core.search.aggregation.CardinalityAggregation;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import io.searchbox.core.search.aggregation.FilterAggregation;
import io.searchbox.core.search.aggregation.HistogramAggregation;
import io.searchbox.core.search.aggregation.MissingAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import io.searchbox.core.search.aggregation.ValueCountAggregation;
import io.searchbox.params.Parameters;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.graylog2.Configuration;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.DateHistogramResult;
import org.graylog2.indexer.results.FieldHistogramResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.results.TermsHistogramResult;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.results.TermsStatsResult;
import org.graylog2.indexer.searches.timeranges.TimeRanges;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.joda.time.Period;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

@Singleton
public class Searches {
    public final static String AGG_TERMS = "gl2_terms";
    public final static String AGG_STATS = "gl2_stats";
    public final static String AGG_TERMS_STATS = "gl2_termsstats";
    public static final String AGG_FILTER = "gl2_filter";
    public static final String AGG_HISTOGRAM = "gl2_histogram";
    public static final String AGG_EXTENDED_STATS = "gl2_extended_stats";
    public static final String AGG_CARDINALITY = "gl2_field_cardinality";
    public static final String AGG_VALUE_COUNT = "gl2_value_count";
    private static final Pattern filterStreamIdPattern = Pattern.compile("^(.+[^\\p{Alnum}])?streams:([\\p{XDigit}]+)");

    // This is the "WORD SEPARATOR MIDDLE DOT" unicode character. It's used to join and split the term values in a
    // stacked terms query.
    public static final String STACKED_TERMS_AGG_SEPARATOR = "\u2E31";

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

        @SuppressWarnings("ImmutableEnumChecker")
        private final Period period;

        DateHistogramInterval(Period period) {
            this.period = period;
        }

        public Period getPeriod() {
            return period;
        }

        public org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval toESInterval() {
            switch (this) {
                case MINUTE:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.MINUTE;
                case HOUR:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.HOUR;
                case DAY:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.DAY;
                case WEEK:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.WEEK;
                case MONTH:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.MONTH;
                case QUARTER:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.QUARTER;
                default:
                    return org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval.YEAR;
            }
        }
    }

    private final Configuration configuration;
    private final IndexRangeService indexRangeService;
    private final Timer esRequestTimer;
    private final Histogram esTimeRangeHistogram;
    private final Counter esTotalSearchesCounter;
    private final StreamService streamService;
    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final JestClient jestClient;
    private final ScrollResult.Factory scrollResultFactory;
    private final Duration esRequestTimeout;

    @Inject
    public Searches(Configuration configuration,
                    IndexRangeService indexRangeService,
                    MetricRegistry metricRegistry,
                    StreamService streamService,
                    Indices indices,
                    IndexSetRegistry indexSetRegistry,
                    JestClient jestClient,
                    ScrollResult.Factory scrollResultFactory,
                    @Named("elasticsearch_request_timeout") Duration requestTimeout) {
        this.configuration = requireNonNull(configuration, "configuration");
        this.indexRangeService = requireNonNull(indexRangeService, "indexRangeService");
        this.esRequestTimeout = requestTimeout;

        this.esRequestTimer = metricRegistry.timer(name(Searches.class, "elasticsearch", "requests"));
        this.esTimeRangeHistogram = metricRegistry.histogram(name(Searches.class, "elasticsearch", "ranges"));
        this.esTotalSearchesCounter = metricRegistry.counter(name(Searches.class, "elasticsearch", "total-searches"));
        this.streamService = requireNonNull(streamService, "streamService");
        this.indices = requireNonNull(indices, "indices");
        this.indexSetRegistry = requireNonNull(indexSetRegistry, "indexSetRegistry");
        this.jestClient = requireNonNull(jestClient, "jestClient");
        this.scrollResultFactory = requireNonNull(scrollResultFactory, "scrollResultFactory");
    }

    public CountResult count(String query, TimeRange range) {
        return count(query, range, null);
    }

    public CountResult count(String query, TimeRange range, String filter) {
        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        if (affectedIndices.isEmpty()) {
            return CountResult.empty();
        }

        final String searchSource = standardSearchRequest(query, 0, -1, range, filter, null, false).toString();
        final Search search = new Search.Builder(searchSource).addIndex(affectedIndices).build();
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(search, () -> "Unable to perform count query");

        recordEsMetrics(searchResult, range);

        return CountResult.create(searchResult.getTotal(), 0);
    }

    public ScrollResult scroll(String query, TimeRange range, int limit, int offset, List<String> fields, String filter) {
        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        final Set<String> indexWildcards = indexSetRegistry.getForIndices(affectedIndices).stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());

        final String searchQuery;
        final Sorting sorting = new Sorting("_doc", Sorting.Direction.ASC);
        if (filter == null) {
            searchQuery = standardSearchRequest(query, limit, offset, range, sorting).toString();
        } else {
            searchQuery = filteredSearchRequest(query, filter, limit, offset, range, sorting).toString();
        }

        final Search.Builder initialSearchBuilder = new Search.Builder(searchQuery)
            .addType(IndexMapping.TYPE_MESSAGE)
            .setParameter(Parameters.SCROLL, "1m")
            .addIndex(indexWildcards);
        fields.forEach(initialSearchBuilder::addSourceIncludePattern);
        final io.searchbox.core.SearchResult initialResult = checkForFailedShards(JestUtils.execute(jestClient, initialSearchBuilder.build(), () -> "Unable to perform scroll search"));

        recordEsMetrics(initialResult, range);

        return scrollResultFactory.create(initialResult, query, fields);
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

    @SuppressWarnings("unchecked")
    public SearchResult search(SearchesConfig config) {
        final Set<IndexRange> indexRanges = determineAffectedIndicesWithRanges(config.range(), config.filter());

        final SearchSourceBuilder requestBuilder = searchRequest(config);
        if (indexRanges.isEmpty()) {
            return SearchResult.empty(config.query(), requestBuilder.toString());
        }

        final Set<String> indices = extractIndexNamesFromIndexRanges(indexRanges);
        final Search.Builder searchBuilder = new Search.Builder(requestBuilder.toString())
            .addType(IndexMapping.TYPE_MESSAGE)
            .addIndex(indices);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform search query");

        final List<ResultMessage> hits = searchResult.getHits(Map.class, false).stream()
            .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source, hit.highlight))
            .collect(Collectors.toList());

        recordEsMetrics(searchResult, config.range());

        return new SearchResult(hits, searchResult.getTotal(), indexRanges, config.query(), requestBuilder.toString(), tookMsFromSearchResult(searchResult));
    }

    public AbstractAggregationBuilder createTermsBuilder(String field, List<String> stackedFields, int size, Terms.Order termsOrder) {
        if (stackedFields.isEmpty()) {
            // Wrap terms aggregation in a no-op filter to make sure the result structure is correct when not having
            // stacked fields.
            return AggregationBuilders.filter(AGG_FILTER, QueryBuilders.matchAllQuery())
                    .subAggregation(AggregationBuilders.terms(AGG_TERMS)
                            .field(field)
                            .size(size > 0 ? size : 50)
                            .order(termsOrder));
        }

        // If the methods gets stacked fields, we have to use scripting to concatenate the fields.
        // There is currently no other way to do this. (as of ES 5.6)
        final StringBuilder scriptStringBuilder = new StringBuilder();

        // Build a filter for the terms aggregation to make sure we only get terms for messages where all fields
        // exist.
        final BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();

        // Add the main field
        scriptStringBuilder.append("doc['").append(field).append("'].value");
        filterQuery.must(QueryBuilders.existsQuery(field));

        // Add all other fields
        stackedFields.forEach(f -> {
            // There is no way to use some kind of structured value for the stacked fields in the painless script
            // so we have to use a "special" character (that is hopefully not showing up in any value) to join the
            // stacked field values. That allows us to split the result again later to create a field->value mapping.
            scriptStringBuilder.append(" + \"").append(STACKED_TERMS_AGG_SEPARATOR).append("\" + ");
            scriptStringBuilder.append("doc['").append(f).append("'].value");
            filterQuery.must(QueryBuilders.existsQuery(f));
        });

        return AggregationBuilders.filter(AGG_FILTER, filterQuery)
                .subAggregation(AggregationBuilders.terms(AGG_TERMS)
                        .script(new Script(ScriptType.INLINE, "painless", scriptStringBuilder.toString(),  Collections.emptyMap()))
                        .size(size > 0 ? size : 50)
                        .order(termsOrder));
    }

    public TermsResult terms(String field, List<String> stackedFields, int size, String query, String filter, TimeRange range, Sorting.Direction sorting) {
        final Terms.Order termsOrder = sorting == Sorting.Direction.DESC ? Terms.Order.count(false) : Terms.Order.count(true);

        final SearchSourceBuilder searchSourceBuilder = filteredSearchRequest(query, filter, range);

        searchSourceBuilder.aggregation(createTermsBuilder(field, stackedFields, size, termsOrder));
        searchSourceBuilder.aggregation(AggregationBuilders.missing("missing")
                        .field(field));

        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        if (affectedIndices.isEmpty()) {
            return TermsResult.empty(query, searchSourceBuilder.toString());
        }
        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
            .ignoreUnavailable(true)
            .allowNoIndices(true)
            .addType(IndexMapping.TYPE_MESSAGE)
            .addIndex(affectedIndices);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform terms query");

        recordEsMetrics(searchResult, range);

        final TermsAggregation termsAggregation = searchResult.getAggregations().getFilterAggregation(AGG_FILTER).getTermsAggregation(AGG_TERMS);
        final MissingAggregation missing = searchResult.getAggregations().getMissingAggregation("missing");

        return new TermsResult(
                termsAggregation,
                missing.getMissing(),
                searchResult.getTotal(),
                query,
                searchSourceBuilder.toString(),
                tookMsFromSearchResult(searchResult),
                // Concat field and stacked fields into one fields list
                ImmutableList.<String>builder().add(field).addAll(stackedFields).build()
        );
    }

    public TermsResult terms(String field, int size, String query, String filter, TimeRange range, Sorting.Direction sorting) {
        return terms(field, Collections.emptyList(), size, query, filter, range, sorting);
    }

    public TermsResult terms(String field, int size, String query, String filter, TimeRange range) {
        return terms(field, size, query, filter, range, Sorting.Direction.DESC);
    }

    public TermsResult terms(String field, int size, String query, TimeRange range) {
        return terms(field, size, query, null, range, Sorting.Direction.DESC);
    }

    public TermsHistogramResult termsHistogram(String field,
                                               List<String> stackedFields,
                                               int size,
                                               String query,
                                               String filter,
                                               TimeRange range,
                                               DateHistogramInterval interval,
                                               Sorting.Direction sorting) {
        final Terms.Order termsOrder = sorting == Sorting.Direction.DESC ? Terms.Order.count(false) : Terms.Order.count(true);

        final DateHistogramAggregationBuilder histogramBuilder = AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(interval.toESInterval())
                .subAggregation(createTermsBuilder(field, stackedFields, size, termsOrder))
                .subAggregation(AggregationBuilders.missing("missing").field(field));

        final SearchSourceBuilder searchSourceBuilder = filteredSearchRequest(query, filter, range)
                .aggregation(histogramBuilder);

        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        if (affectedIndices.isEmpty()) {
            return TermsHistogramResult.empty(query, searchSourceBuilder.toString(), size, interval);
        }
        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform terms query");

        recordEsMetrics(searchResult, range);

        final DateHistogramAggregation dateHistogramAggregation = searchResult.getAggregations().getDateHistogramAggregation(AGG_HISTOGRAM);

        return new TermsHistogramResult(
                dateHistogramAggregation,
                query,
                searchSourceBuilder.toString(),
                size,
                tookMsFromSearchResult(searchResult),
                interval,
                // Concat field and stacked fields into one fields list
                ImmutableList.<String>builder().add(field).addAll(stackedFields).build());
    }

    public TermsStatsResult termsStats(String keyField, String valueField, TermsStatsOrder order, int size, String query, String filter, TimeRange range) {
        if (size == 0) {
            size = 50;
        }

        final Set<String> affectedIndices = determineAffectedIndices(range, filter);

        final SearchSourceBuilder searchSourceBuilder;
        if (filter == null) {
            searchSourceBuilder = standardSearchRequest(query, range);
        } else {
            searchSourceBuilder = filteredSearchRequest(query, filter, range);
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

        final FilterAggregationBuilder builder = AggregationBuilders.filter(AGG_FILTER, standardAggregationFilters(range, filter))
            .subAggregation(
                AggregationBuilders.terms(AGG_TERMS_STATS)
                    .field(keyField)
                    .subAggregation(AggregationBuilders.stats(AGG_STATS).field(valueField))
                    .order(termsOrder)
                    .size(size)
            );
        searchSourceBuilder.aggregation(builder);

        if (affectedIndices.isEmpty()) {
            return TermsStatsResult.empty(query, searchSourceBuilder.toString());
        }
        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
            .addType(IndexMapping.TYPE_MESSAGE)
            .addIndex(affectedIndices);

        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to retrieve terms stats");

        final FilterAggregation filterAggregation = searchResult.getAggregations().getFilterAggregation(AGG_FILTER);
        final TermsAggregation termsAggregation = filterAggregation.getTermsAggregation(AGG_TERMS_STATS);

        recordEsMetrics(searchResult, range);

        return new TermsStatsResult(
            termsAggregation,
            query,
            searchSourceBuilder.toString(),
            tookMsFromSearchResult(searchResult)
        );
    }

    public TermsStatsResult termsStats(String keyField, String valueField, TermsStatsOrder order, int size, String query, TimeRange range) {
        return termsStats(keyField, valueField, order, size, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, TimeRange range) {
        return fieldStats(field, query, null, range);
    }

    public FieldStatsResult fieldStats(String field, String query, String filter, TimeRange range) {
        // by default include the cardinality aggregation, as well.
        return fieldStats(field, query, filter, range, true, true, true);
    }

    @SuppressWarnings("unchecked")
    public FieldStatsResult fieldStats(String field,
                                       String query,
                                       String filter,
                                       TimeRange range,
                                       boolean includeCardinality,
                                       boolean includeStats,
                                       boolean includeCount) {
        final SearchSourceBuilder searchSourceBuilder;
        if (filter == null) {
            searchSourceBuilder = standardSearchRequest(query, range);
        } else {
            searchSourceBuilder = filteredSearchRequest(query, filter, range);
        }

        final FilterAggregationBuilder filterBuilder = AggregationBuilders.filter(AGG_FILTER, standardAggregationFilters(range, filter));
        if (includeCount) {
            searchSourceBuilder.aggregation(AggregationBuilders.count(AGG_VALUE_COUNT).field(field));
        }
        if (includeStats) {
            searchSourceBuilder.aggregation(AggregationBuilders.extendedStats(AGG_EXTENDED_STATS).field(field));
        }
        if (includeCardinality) {
            searchSourceBuilder.aggregation(AggregationBuilders.cardinality(AGG_CARDINALITY).field(field));
        }

        searchSourceBuilder.aggregation(filterBuilder);

        final Set<String> indices = indicesContainingField(determineAffectedIndices(range, filter), field);
        if (indices.isEmpty()) {
            return FieldStatsResult.empty(query, searchSourceBuilder.toString());
        }

        final Search searchRequest = new Search.Builder(searchSourceBuilder.toString())
            .addType(IndexMapping.TYPE_MESSAGE)
            .addIndex(indices)
                .build();
        final io.searchbox.core.SearchResult searchResponse = wrapInMultiSearch(searchRequest, () -> "Unable to retrieve fields stats");
        final List<ResultMessage> hits = searchResponse.getHits(Map.class, false).stream()
            .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source))
            .collect(Collectors.toList());

        recordEsMetrics(searchResponse, range);

        final ExtendedStatsAggregation extendedStatsAggregation = searchResponse.getAggregations().getExtendedStatsAggregation(AGG_EXTENDED_STATS);
        final ValueCountAggregation valueCountAggregation = searchResponse.getAggregations().getValueCountAggregation(AGG_VALUE_COUNT);
        final CardinalityAggregation cardinalityAggregation = searchResponse.getAggregations().getCardinalityAggregation(AGG_CARDINALITY);

        return new FieldStatsResult(
                valueCountAggregation,
                extendedStatsAggregation,
                cardinalityAggregation,
                hits,
                query,
                searchSourceBuilder.toString(),
                tookMsFromSearchResult(searchResponse)
        );
    }

    private Set<String> indicesContainingField(Set<String> strings, String field) {
        return indices.getAllMessageFieldsForIndices(strings.toArray(new String[strings.size()]))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().contains(field))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public HistogramResult histogram(String query, DateHistogramInterval interval, TimeRange range) {
        return histogram(query, interval, null, range);
    }

    public HistogramResult histogram(String query, DateHistogramInterval interval, String filter, TimeRange range) {
        final DateHistogramAggregationBuilder histogramBuilder = AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(interval.toESInterval());

        final SearchSourceBuilder searchSourceBuilder = filteredSearchRequest(query, filter, range)
            .aggregation(histogramBuilder);

        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        if (affectedIndices.isEmpty()) {
            return DateHistogramResult.empty(query, searchSourceBuilder.toString(), interval);
        }

        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
            .addType(IndexMapping.TYPE_MESSAGE)
            .addIndex(affectedIndices)
            .ignoreUnavailable(true)
            .allowNoIndices(true);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to retrieve histogram");

        recordEsMetrics(searchResult, range);

        final HistogramAggregation histogramAggregation = searchResult.getAggregations().getHistogramAggregation(AGG_HISTOGRAM);

        return new DateHistogramResult(
            histogramAggregation,
            query,
            searchSourceBuilder.toString(),
            interval,
            tookMsFromSearchResult(searchResult)
        );
    }

    public HistogramResult fieldHistogram(String query,
                                          String field,
                                          DateHistogramInterval interval,
                                          String filter,
                                          TimeRange range,
                                          boolean includeCardinality) {
        return fieldHistogram(query, field, interval, filter, range, true, includeCardinality);
    }

    public HistogramResult fieldHistogram(String query,
                                          String field,
                                          DateHistogramInterval interval,
                                          String filter,
                                          TimeRange range,
                                          boolean includeStats,
                                          boolean includeCardinality) {
        final DateHistogramAggregationBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(interval.toESInterval());

        if (includeStats) {
            dateHistogramBuilder.subAggregation(AggregationBuilders.stats(AGG_STATS).field(field));
        } else {
            // Stats aggregation already include count. Only calculate it explicitly when stats are disabled
            dateHistogramBuilder.subAggregation(AggregationBuilders.count(AGG_VALUE_COUNT).field(field));
        }
        if (includeCardinality) {
            dateHistogramBuilder.subAggregation(AggregationBuilders.cardinality(AGG_CARDINALITY).field(field));
        }

        final SearchSourceBuilder searchSourceBuilder = filteredSearchRequest(query, filter, range)
            .aggregation(dateHistogramBuilder);

        final Set<String> affectedIndices = determineAffectedIndices(range, filter);
        if (affectedIndices.isEmpty()) {
            return FieldHistogramResult.empty(query, searchSourceBuilder.toString(), interval);
        }
        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
            .addType(IndexMapping.TYPE_MESSAGE)
            .addIndex(affectedIndices);

        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to retrieve field histogram");

        recordEsMetrics(searchResult, range);

        final HistogramAggregation histogramAggregation = searchResult.getAggregations().getHistogramAggregation(AGG_HISTOGRAM);

        return new FieldHistogramResult(
                histogramAggregation,
                query,
                searchSourceBuilder.toString(),
                interval,
                tookMsFromSearchResult(searchResult));
    }

    private <T extends JestResult> T checkForFailedShards(T result) throws FieldTypeException {
        // unwrap shard failure due to non-numeric mapping. this happens when searching across index sets
        // if at least one of the index sets comes back with a result, the overall result will have the aggregation
        // but not considered failed entirely. however, if one shard has the error, we will refuse to respond
        // otherwise we would be showing empty graphs for non-numeric fields.
        final JsonNode shards = result.getJsonObject().path("_shards");
        final double failedShards = shards.path("failed").asDouble();

        if (failedShards > 0) {
            final SearchFailure searchFailure = new SearchFailure(shards);
            final List<String> nonNumericFieldErrors = searchFailure.getNonNumericFieldErrors();

            if (!nonNumericFieldErrors.isEmpty()) {
                throw new FieldTypeException("Unable to perform search query", nonNumericFieldErrors);
            }

            throw new ElasticsearchException("Unable to perform search query", searchFailure.getErrors());
        }

        return result;
    }

    private SearchSourceBuilder searchRequest(SearchesConfig config) {
        final SearchSourceBuilder request;

        if (config.filter() == null) {
            request = standardSearchRequest(config.query(), config.limit(), config.offset(), config.range(), config.sorting());
        } else {
            request = filteredSearchRequest(config.query(), config.filter(), config.limit(), config.offset(), config.range(), config.sorting());
        }

        final List<String> fields = config.fields();
        if (fields != null) {
            // Use source filtering instead of SearchSourceBuilder#fields() here because Jest cannot handle responses
            // without a "_source" field yet. See:
            // https://github.com/searchbox-io/Jest/issues/157
            // https://github.com/searchbox-io/Jest/issues/339
            request.fetchSource(fields.toArray(new String[fields.size()]), Strings.EMPTY_ARRAY);
        }

        return request;
    }

    private SearchSourceBuilder standardSearchRequest(String query, TimeRange range) {
        return standardSearchRequest(query, 0, 0, range, null);
    }

    private SearchSourceBuilder standardSearchRequest(String query,
                                                      int limit,
                                                      int offset,
                                                      TimeRange range,
                                                      Sorting sort) {
        return standardSearchRequest(query, limit, offset, range, sort, true);
    }

    private SearchSourceBuilder standardSearchRequest(
            String query,
            int limit,
            int offset,
            TimeRange range,
            Sorting sort,
            boolean highlight) {
        return standardSearchRequest(query, limit, offset, range, null, sort, highlight);
    }

    private SearchSourceBuilder standardSearchRequest(
            String query,
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
        if ("*".equals(query.trim())) {
            queryBuilder = matchAllQuery();
        } else {
            queryBuilder = queryStringQuery(query).allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());
        }

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .query(QueryBuilders.boolQuery().must(queryBuilder).filter(standardFilters(range, filter)))
            .timeout(new TimeValue(esRequestTimeout.toMilliseconds(), TimeUnit.MILLISECONDS));

        if (offset >= 0) {
            searchSourceBuilder.from(offset);
        }

        if (limit > 0) {
            searchSourceBuilder.size(limit);
        }

        if (sort != null) {
            searchSourceBuilder.sort(sort.getField(), sort.asElastic());
        }

        if (highlight && configuration.isAllowHighlighting()) {
            final HighlightBuilder highlightBuilder = new HighlightBuilder()
                .requireFieldMatch(false)
                .field("*")
                .fragmentSize(0)
                .numOfFragments(0);
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        return searchSourceBuilder;
    }

    private SearchSourceBuilder filteredSearchRequest(String query, String filter, TimeRange range) {
        return filteredSearchRequest(query, filter, 0, 0, range, null);
    }

    private SearchSourceBuilder filteredSearchRequest(String query, String filter, int limit, int offset, TimeRange range, Sorting sort) {
        return standardSearchRequest(query, limit, offset, range, filter, sort, true);
    }

    protected long tookMsFromSearchResult(JestResult searchResult) {
        final JsonNode tookMs = searchResult.getJsonObject().path("took");
        if (tookMs.isNumber()) {
            return tookMs.asLong();
        } else {
            throw new ElasticsearchException("Unexpected response structure: " + searchResult.getJsonString());
        }
    }

    private void recordEsMetrics(JestResult jestResult, @Nullable TimeRange range) {
        esTotalSearchesCounter.inc();

        final long tookMs = tookMsFromSearchResult(jestResult);
        esRequestTimer.update(tookMs, TimeUnit.MILLISECONDS);

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
        if (!isNullOrEmpty(filter) && !"*".equals(filter)) {
            if (bfb == null) {
                bfb = QueryBuilders.boolQuery();
            }
            bfb.must(queryStringQuery(filter));
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
     * @param filter the filter string like "streams:xxxyyyzzz"
     * @return the optional stream id
     */
    public static Optional<String> extractStreamId(String filter) {
        if (isNullOrEmpty(filter)) {
            return Optional.empty();
        }
        final Matcher streamIdMatcher = filterStreamIdPattern.matcher(filter);
        if (streamIdMatcher.find()) {
            return Optional.of(streamIdMatcher.group(2));
        }
        return Optional.empty();
    }

    @VisibleForTesting
    Set<String> determineAffectedIndices(TimeRange range, @Nullable String filter) {
        return extractIndexNamesFromIndexRanges(determineAffectedIndicesWithRanges(range, filter));
    }

    private Set<String> extractIndexNamesFromIndexRanges(Set<IndexRange> indexRanges) {
        return indexRanges.stream()
                .map(IndexRange::indexName)
                .collect(Collectors.toSet());
    }

    @VisibleForTesting
    Set<IndexRange> determineAffectedIndicesWithRanges(TimeRange range, @Nullable String filter) {
        final Optional<String> streamId = extractStreamId(filter);
        IndexSet indexSet = null;
        // if we are searching in a stream, we are further restricting the indices using the currently
        // configure index set of that stream.
        // later on we will also test against each index range (we load all of them) to see if there are
        // additional index ranges that match, this can happen with restored archives or when the index set for
        // a stream has changed: a stream only knows about its currently configured index set, no the history
        if (streamId.isPresent()) {
            try {
                final Stream stream = streamService.load(streamId.get());
                indexSet = stream.getIndexSet();
            } catch (NotFoundException ignored) {
            }
        }

        final ImmutableSortedSet.Builder<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        final SortedSet<IndexRange> indexRanges = indexRangeService.find(range.getFrom(), range.getTo());
        final Set<String> affectedIndexNames = indexRanges.stream().map(IndexRange::indexName).collect(Collectors.toSet());
        final Set<IndexSet> eventIndexSets = indexSetRegistry.getForIndices(affectedIndexNames).stream()
                .filter(indexSet1 -> IndexSetConfig.TemplateType.EVENTS.equals(indexSet1.getConfig().indexTemplateType().orElse(IndexSetConfig.TemplateType.MESSAGES)))
                .collect(Collectors.toSet());
        for (IndexRange indexRange : indexRanges) {
            // if we aren't in a stream search, we look at all the ranges matching the time range.
            if (indexSet == null && filter == null) {
                // Don't include the index range if it's for an event index set to avoid sorting issues.
                // See the following issues for details:
                // - https://github.com/Graylog2/graylog2-server/issues/6384
                // - https://github.com/Graylog2/graylog2-server/issues/6490
                if (eventIndexSets.stream().anyMatch(set -> set.isManagedIndex(indexRange.indexName()))) {
                    continue;
                }
                indices.add(indexRange);
                continue;
            }

            // A range applies to this search if either: the current index set of the stream matches or a previous index set matched.
            final boolean streamInIndexRange = streamId.isPresent() && indexRange.streamIds() != null && indexRange.streamIds().contains(streamId.get());
            final boolean streamInCurrentIndexSet = indexSet != null && indexSet.isManagedIndex(indexRange.indexName());

            if (streamInIndexRange) {
                indices.add(indexRange);
            }
            if (streamInCurrentIndexSet) {
                indices.add(indexRange);
            }
        }

        return indices.build();
    }

    protected io.searchbox.core.SearchResult wrapInMultiSearch(Search search, Supplier<String> errorMessage) {
        final MultiSearch multiSearch = new MultiSearch.Builder(search).build();
        final MultiSearchResult multiSearchResult = JestUtils.execute(jestClient, multiSearch, errorMessage);

        final List<MultiSearchResult.MultiSearchResponse> responses = multiSearchResult.getResponses();
        if (responses.size() != 1) {
            throw new ElasticsearchException("Expected exactly 1 search result, but got " + responses.size());
        }

        final MultiSearchResult.MultiSearchResponse response = responses.get(0);
        if (response.isError) {
            throw JestUtils.specificException(errorMessage, response.error);
        }

        return checkForFailedShards(response.searchResult);
    }
}
