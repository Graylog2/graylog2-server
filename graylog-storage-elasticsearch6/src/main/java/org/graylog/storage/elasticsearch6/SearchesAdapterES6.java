package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
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
import io.searchbox.core.search.sort.Sort;
import io.searchbox.params.Parameters;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.search.MoreSearch;
import org.graylog2.Configuration;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.ranges.IndexRange;
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
import org.graylog2.indexer.searches.SearchFailure;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class SearchesAdapterES6 implements SearchesAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SearchesAdapterES6.class);
    public static final String AGG_CARDINALITY = "gl2_field_cardinality";
    public static final String AGG_HISTOGRAM = "gl2_histogram";
    public static final String AGG_EXTENDED_STATS = "gl2_extended_stats";
    public static final String AGG_FILTER = "gl2_filter";
    public final static String AGG_STATS = "gl2_stats";
    public final static String AGG_TERMS = "gl2_terms";
    public final static String AGG_TERMS_STATS = "gl2_termsstats";
    public static final String AGG_VALUE_COUNT = "gl2_value_count";

    // This is the "WORD SEPARATOR MIDDLE DOT" unicode character. It's used to join and split the term values in a
    // stacked terms query.
    public static final String STACKED_TERMS_AGG_SEPARATOR = "\u2E31";
    private final JestClient jestClient;
    private final Configuration configuration;
    private final ScrollResult.Factory scrollResultFactory;

    @Inject
    public SearchesAdapterES6(JestClient jestClient, Configuration configuration, ScrollResult.Factory scrollResultFactory) {
        this.jestClient = jestClient;
        this.configuration = configuration;
        this.scrollResultFactory = scrollResultFactory;
    }

    @Override
    public CountResult count(Set<String> affectedIndices, String query, TimeRange range, String filter) {
        final String searchSource = standardSearchRequest(query, 0, -1, range, filter, null, false).toString();
        final Search search = new Search.Builder(searchSource).addIndex(affectedIndices).build();
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(search, () -> "Unable to perform count query");

        final long tookMs = tookMsFromSearchResult(searchResult);
        return CountResult.create(searchResult.getTotal(), tookMs);
    }

    @Override
    public ScrollResult scroll(Set<String> affectedIndices, Set<String> indexWildcards, Sorting sorting, String filter, String query, TimeRange range, int limit, int offset, List<String> fields) {
        final String searchQuery;

        if (filter == null) {
            searchQuery = standardSearchRequest(query, limit, offset, range, sorting, configuration.isAllowHighlighting()).toString();
        } else {
            searchQuery = filteredSearchRequest(query, filter, limit, offset, range, sorting).toString();
        }

        final Search.Builder initialSearchBuilder = new Search.Builder(searchQuery)
                .addType(IndexMapping.TYPE_MESSAGE)
                .setParameter(Parameters.SCROLL, "1m")
                .addIndex(indexWildcards);
        fields.forEach(initialSearchBuilder::addSourceIncludePattern);
        final io.searchbox.core.SearchResult initialResult = checkForFailedShards(JestUtils.execute(jestClient, initialSearchBuilder.build(), () -> "Unable to perform scroll search"));

        return scrollResultFactory.create(initialResult, query, fields);
    }

    @Override
    public SearchResult search(Set<String> indices, Set<IndexRange> indexRanges, SearchesConfig config) {
        final SearchSourceBuilder requestBuilder = searchRequest(config);
        if (indexRanges.isEmpty()) {
            return SearchResult.empty(config.query(), requestBuilder.toString());
        }
        final Search.Builder searchBuilder = new Search.Builder(requestBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(indices);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform search query");

        final List<ResultMessage> hits = searchResult.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source, hit.highlight))
                .collect(Collectors.toList());


        return new SearchResult(hits, searchResult.getTotal(), indexRanges, config.query(), requestBuilder.toString(), tookMsFromSearchResult(searchResult));

    }

    @Override
    public TermsResult terms(String query, String filter, TimeRange range, Set<String> affectedIndices, String field, List<String> stackedFields, int size, Sorting.Direction sorting) {
        final Terms.Order termsOrder = sorting == Sorting.Direction.DESC ? Terms.Order.count(false) : Terms.Order.count(true);
        final SearchSourceBuilder searchSourceBuilder = filteredSearchRequest(query, filter, range);

        searchSourceBuilder.aggregation(createTermsBuilder(field, stackedFields, size, termsOrder));
        searchSourceBuilder.aggregation(AggregationBuilders.missing("missing")
                .field(field));

        if (affectedIndices.isEmpty()) {
            return TermsResult.empty(query, searchSourceBuilder.toString());
        }
        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform terms query");
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

    @Override
    public TermsHistogramResult termsHistogram(String query, String filter, TimeRange range, Set<String> affectedIndices, String field, List<String> stackedFields, int size, Sorting.Direction sorting, Searches.DateHistogramInterval interval) {
        final Terms.Order termsOrder = sorting == Sorting.Direction.DESC ? Terms.Order.count(false) : Terms.Order.count(true);

        final DateHistogramAggregationBuilder histogramBuilder = AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(toESInterval(interval))
                .subAggregation(createTermsBuilder(field, stackedFields, size, termsOrder))
                .subAggregation(AggregationBuilders.missing("missing").field(field));

        final SearchSourceBuilder searchSourceBuilder = filteredSearchRequest(query, filter, range)
                .aggregation(histogramBuilder);

        if (affectedIndices.isEmpty()) {
            return TermsHistogramResult.empty(query, searchSourceBuilder.toString(), size, interval);
        }
        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform terms query");
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

    @Override
    public TermsStatsResult termsStats(String query, String filter, TimeRange range, Set<String> affectedIndices, String keyField, String valueField, Searches.TermsStatsOrder order, int size) {
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


        return new TermsStatsResult(
                termsAggregation,
                query,
                searchSourceBuilder.toString(),
                tookMsFromSearchResult(searchResult)
        );
    }

    @Override
    public FieldStatsResult fieldStats(String query, String filter, TimeRange range, Set<String> indices, String field, boolean includeCardinality, boolean includeStats, boolean includeCount) {

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

    @Override
    public HistogramResult histogram(String query, String filter, TimeRange range, Set<String> affectedIndices, Searches.DateHistogramInterval interval) {
        final DateHistogramAggregationBuilder histogramBuilder = AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(toESInterval(interval));

        final SearchSourceBuilder searchSourceBuilder = filteredSearchRequest(query, filter, range)
                .aggregation(histogramBuilder);

        if (affectedIndices.isEmpty()) {
            return DateHistogramResult.empty(query, searchSourceBuilder.toString(), interval);
        }

        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices)
                .ignoreUnavailable(true)
                .allowNoIndices(true);
        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to retrieve histogram");


        final HistogramAggregation histogramAggregation = searchResult.getAggregations().getHistogramAggregation(AGG_HISTOGRAM);

        return new DateHistogramResult(
                histogramAggregation,
                query,
                searchSourceBuilder.toString(),
                interval,
                tookMsFromSearchResult(searchResult)
        );
    }

    @Override
    public HistogramResult fieldHistogram(String query, String filter, TimeRange range, Set<String> affectedIndices, String field, Searches.DateHistogramInterval interval, boolean includeStats, boolean includeCardinality) {
        final DateHistogramAggregationBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(AGG_HISTOGRAM)
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(toESInterval(interval));

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

        if (affectedIndices.isEmpty()) {
            return FieldHistogramResult.empty(query, searchSourceBuilder.toString(), interval);
        }
        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices);

        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to retrieve field histogram");

        final HistogramAggregation histogramAggregation = searchResult.getAggregations().getHistogramAggregation(AGG_HISTOGRAM);

        return new FieldHistogramResult(
                histogramAggregation,
                query,
                searchSourceBuilder.toString(),
                interval,
                tookMsFromSearchResult(searchResult));
    }

    @Override
    public MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices, Sorting sorting, int page, int perPage, Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams) {
        final QueryBuilder query = (queryString.isEmpty() || queryString.equals("*")) ?
                matchAllQuery() :
                queryStringQuery(queryString).allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        final BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(termsQuery(EventDto.FIELD_STREAMS, eventStreams))
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(timerange)));

        if (!isNullOrEmpty(filterString)) {
            filter.filter(queryStringQuery(filterString));
        }

        if (!forbiddenSourceStreams.isEmpty()) {
            // If an event has any stream in "source_streams" that the calling search user is not allowed to access,
            // the event must not be in the search result.
            filter.filter(boolQuery().mustNot(termsQuery(EventDto.FIELD_SOURCE_STREAMS, forbiddenSourceStreams)));
        }

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(filter)
                .from((page - 1) * perPage)
                .size(perPage)
                .sort(sorting.getField(), sorting.asElastic());

        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices)
                .allowNoIndices(false)
                .ignoreUnavailable(false);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Query:\n{}", searchSourceBuilder.toString(new ToXContent.MapParams(Collections.singletonMap("pretty", "true"))));
            LOG.debug("Execute search: {}", searchBuilder.build().toString());
        }

        final io.searchbox.core.SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform search query");

        @SuppressWarnings("unchecked") final List<ResultMessage> hits = searchResult.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source, hit.highlight))
                .collect(Collectors.toList());

        return MoreSearch.Result.builder()
                .results(hits)
                .resultsCount(searchResult.getTotal())
                .duration(tookMsFromSearchResult(searchResult))
                .usedIndexNames(affectedIndices)
                .executedQuery(searchSourceBuilder.toString())
                .build();
    }

    @Override
    public void scrollEvents(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams, String scrollTime, int batchSize, ScrollEventsCallback resultCallback) throws EventProcessorException {
        final QueryBuilder query = (queryString.trim().isEmpty() || queryString.trim().equals("*")) ?
                matchAllQuery() :
                queryStringQuery(queryString).allowLeadingWildcard(configuration.isAllowLeadingWildcardSearches());

        final BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(timeRange)));

        // Filtering with an empty streams list doesn't work and would return zero results
        if (!streams.isEmpty()) {
            filter.filter(termsQuery(Message.FIELD_STREAMS, streams));
        }

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(filter)
                .size(batchSize);

        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                // Scroll requests contain the indices in the URL. If the list of indices is too long, the request can
                // fail. There is no way of executing a scroll search without having the list of indices in the URL,
                // as of this writing. (ES 6.8/7.1)
                .addIndex(affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices)
                // For correlation need the oldest messages to come in first
                .addSort(new Sort("timestamp", Sort.Sorting.ASC))
                .allowNoIndices(false)
                .ignoreUnavailable(false)
                .setParameter(Parameters.SCROLL, scrollTime);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Query:\n{}", searchSourceBuilder.toString(new ToXContent.MapParams(Collections.singletonMap("pretty", "true"))));
            LOG.debug("Execute search: {}", searchBuilder.build().toString());
        }

        final io.searchbox.core.SearchResult result = JestUtils.execute(jestClient, searchBuilder.build(), () -> "Unable to scroll indices.");

        final ScrollResult scrollResult = scrollResultFactory.create(result, searchSourceBuilder.toString(), scrollTime, Collections.emptyList());
        final AtomicBoolean continueScrolling = new AtomicBoolean(true);

        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            ScrollResult.ScrollChunk scrollChunk = scrollResult.nextChunk();
            while (continueScrolling.get() && scrollChunk != null) {
                final List<ResultMessage> messages = scrollChunk.getMessages();

                LOG.debug("Passing <{}> messages to callback", messages.size());
                resultCallback.accept(Collections.unmodifiableList(messages), continueScrolling);

                // Stop if the resultCallback told us to stop
                if (!continueScrolling.get()) {
                    break;
                }

                scrollChunk = scrollResult.nextChunk();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                // Tell Elasticsearch that we are done with the scroll so it can release resources as soon as possible
                // instead of waiting for the scroll timeout to kick in.
                scrollResult.cancel();
            } catch (Exception ignored) {
            }
            LOG.debug("Scrolling done - took {} ms", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private QueryBuilder standardAggregationFilters(TimeRange range, String filter) {
        final QueryBuilder filterBuilder = standardFilters(range, filter);

        // Throw an exception here to avoid exposing an internal Elasticsearch exception later.
        if (filterBuilder == null) {
            throw new RuntimeException("Either range or filter must be set.");
        }

        return filterBuilder;
    }

    DateHistogramInterval toESInterval(Searches.DateHistogramInterval interval) {
        switch (interval) {
            case MINUTE:
                return DateHistogramInterval.MINUTE;
            case HOUR:
                return DateHistogramInterval.HOUR;
            case DAY:
                return DateHistogramInterval.DAY;
            case WEEK:
                return DateHistogramInterval.WEEK;
            case MONTH:
                return DateHistogramInterval.MONTH;
            case QUARTER:
                return DateHistogramInterval.QUARTER;
            default:
                return DateHistogramInterval.YEAR;
        }
    }

    protected long tookMsFromSearchResult(JestResult searchResult) {
        final JsonNode tookMs = searchResult.getJsonObject().path("took");
        if (tookMs.isNumber()) {
            return tookMs.asLong();
        } else {
            throw new ElasticsearchException("Unexpected response structure: " + searchResult.getJsonString());
        }
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
                .query(QueryBuilders.boolQuery().must(queryBuilder).filter(standardFilters(range, filter)));

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
}
