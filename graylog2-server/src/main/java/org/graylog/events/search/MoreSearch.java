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
package org.graylog.events.search;

import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.params.Parameters;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexRangeContainsOneOfStreams;
import org.graylog.plugins.views.search.errors.EmptyParameterError;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog2.Configuration;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

/**
 * This class contains search helper for the events system.
 */
public class MoreSearch extends Searches {
    private static final Logger LOG = LoggerFactory.getLogger(MoreSearch.class);

    private final StreamService streamService;
    private final IndexRangeService indexRangeService;
    private final ScrollResult.Factory scrollResultFactory;
    private final JestClient jestClient;
    private final boolean allowLeadingWildcardSearches;
    private final ESQueryDecorators esQueryDecorators;
    private final Duration esRequestTimeout;

    @Inject
    public MoreSearch(StreamService streamService,
                      Indices indices,
                      IndexRangeService indexRangeService,
                      IndexSetRegistry indexSetRegistry,
                      MetricRegistry metricRegistry,
                      ScrollResult.Factory scrollResultFactory,
                      JestClient jestClient,
                      Configuration configuration,
                      ESQueryDecorators esQueryDecorators,
                      @Named("elasticsearch_request_timeout") Duration requestTimeout) {
        super(configuration, indexRangeService, metricRegistry, streamService, indices, indexSetRegistry, jestClient, scrollResultFactory, requestTimeout);
        this.streamService = streamService;
        this.indexRangeService = indexRangeService;
        this.scrollResultFactory = scrollResultFactory;
        this.jestClient = jestClient;
        this.allowLeadingWildcardSearches = configuration.isAllowLeadingWildcardSearches();
        this.esQueryDecorators = esQueryDecorators;
        this.esRequestTimeout = requestTimeout;
    }

    /**
     * Executes an events search for the given parameters.
     *
     * @param parameters             event search parameters
     * @param filterString           filter string
     * @param eventStreams           event streams to search in
     * @param forbiddenSourceStreams forbidden source streams
     * @return the result
     */
    // TODO: We cannot use Searches#search() at the moment because that method cannot handle multiple streams. (because of Searches#extractStreamId())
    //       We also cannot use the new search code at the moment because it doesn't do pagination.
    Result eventSearch(EventsSearchParameters parameters, String filterString, Set<String> eventStreams, Set<String> forbiddenSourceStreams) {
        checkArgument(parameters != null, "parameters cannot be null");
        checkArgument(!eventStreams.isEmpty(), "eventStreams cannot be empty");
        checkArgument(forbiddenSourceStreams != null, "forbiddenSourceStreams cannot be null");

        final Sorting.Direction sortDirection = parameters.sortDirection() == EventsSearchParameters.SortDirection.ASC ? Sorting.Direction.ASC : Sorting.Direction.DESC;
        final Sorting sorting = new Sorting(parameters.sortBy(), sortDirection);
        final String queryString = parameters.query().trim();
        final Set<String> affectedIndices = getAffectedIndices(eventStreams, parameters.timerange());

        final QueryBuilder query = (queryString.isEmpty() || queryString.equals("*")) ?
                matchAllQuery() :
                queryStringQuery(queryString).allowLeadingWildcard(allowLeadingWildcardSearches);

        final BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(termsQuery(EventDto.FIELD_STREAMS, eventStreams))
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(parameters.timerange())));

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
                .from((parameters.page() - 1) * parameters.perPage())
                .size(parameters.perPage())
                .sort(sorting.getField(), sorting.asElastic())
                .timeout(new TimeValue(esRequestTimeout.toMilliseconds(), TimeUnit.MILLISECONDS));

        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices)
                .allowNoIndices(false)
                .ignoreUnavailable(false);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Query:\n{}", searchSourceBuilder.toString(new ToXContent.MapParams(Collections.singletonMap("pretty", "true"))));
            LOG.debug("Execute search: {}", searchBuilder.build().toString());
        }

        final SearchResult searchResult = wrapInMultiSearch(searchBuilder.build(), () -> "Unable to perform search query");

        @SuppressWarnings("unchecked") final List<ResultMessage> hits = searchResult.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source, hit.highlight))
                .collect(Collectors.toList());

        return Result.builder()
                .results(hits)
                .resultsCount(searchResult.getTotal())
                .duration(tookMsFromSearchResult(searchResult))
                .usedIndexNames(affectedIndices)
                .executedQuery(searchSourceBuilder.toString())
                .build();
    }

    private Set<String> getAffectedIndices(Set<String> streamIds, TimeRange timeRange) {
        final SortedSet<IndexRange> indexRanges = indexRangeService.find(timeRange.getFrom(), timeRange.getTo());

        // We support an empty streams list and return all affected indices in that case.
        if (streamIds.isEmpty()) {
            return indexRanges.stream()
                    .map(IndexRange::indexName)
                    .collect(Collectors.toSet());
        } else {
            final Set<Stream> streams = loadStreams(streamIds);
            final IndexRangeContainsOneOfStreams indexRangeContainsOneOfStreams = new IndexRangeContainsOneOfStreams(streams);
            return indexRanges.stream()
                    .filter(indexRangeContainsOneOfStreams)
                    .map(IndexRange::indexName)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * This scrolls results for the given query, streams and time range from Elasticsearch. The result is passed to
     * the given callback in batches. (using the given batch size)
     * <p>
     * The search will continue until it is done, an error occurs or the search is stopped by setting the
     * {@code continueScrolling} boolean to {@code false} from the {@link ScrollCallback}.
     * <p></p>
     * TODO: Elasticsearch has a default limit of 500 concurrent scrolls. Every caller of this method should check
     * if there is capacity to create a new scroll request. This can be done by using the ES nodes stats API.
     * See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html#scroll-search-context
     *
     * @param queryString    the search query string
     * @param streams        the set of streams to search in
     * @param timeRange      the time range for the search
     * @param batchSize      the number of documents to retrieve at once
     * @param resultCallback the callback that gets executed for each batch
     */
    public void scrollQuery(String queryString, Set<String> streams, Set<Parameter> queryParameters, TimeRange timeRange, int batchSize, ScrollCallback resultCallback) throws EventProcessorException {
        final String scrollTime = "1m"; // TODO: Does scroll time need to be configurable?

        final Set<String> affectedIndices = getAffectedIndices(streams, timeRange);

        try {
            queryString = decorateQuery(queryParameters, timeRange, queryString);
        } catch (SearchException e) {
            if (e.error() instanceof EmptyParameterError) {
                LOG.debug("Empty parameter from lookup table. Assuming non-matching query. Error: {}", e.getMessage());
                return;
            }
            throw e;
        }

        final QueryBuilder query = (queryString.trim().isEmpty() || queryString.trim().equals("*")) ?
                matchAllQuery() :
                queryStringQuery(queryString).allowLeadingWildcard(allowLeadingWildcardSearches);

        final BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(requireNonNull(IndexHelper.getTimestampRangeFilter(timeRange)));

        // Filtering with an empty streams list doesn't work and would return zero results
        if (!streams.isEmpty()) {
            filter.filter(termsQuery(Message.FIELD_STREAMS, streams));
        }

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(filter)
                .size(batchSize)
                .timeout(new TimeValue(esRequestTimeout.toMilliseconds(), TimeUnit.MILLISECONDS));

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

        final SearchResult result = JestUtils.execute(jestClient, searchBuilder.build(), () -> "Unable to scroll indices.");

        final ScrollResult scrollResult = scrollResultFactory.create(result, searchSourceBuilder.toString(), scrollTime, Collections.emptyList());
        final AtomicBoolean continueScrolling = new AtomicBoolean(true);

        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            ScrollResult.ScrollChunk scrollChunk = scrollResult.nextChunk();
            while (continueScrolling.get() && scrollChunk != null) {
                final List<ResultMessage> messages = scrollChunk.getMessages();

                LOG.debug("Passing <{}> messages to callback", messages.size());
                resultCallback.call(Collections.unmodifiableList(messages), continueScrolling);

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

    public Set<Stream> loadStreams(Set<String> streamIds) {
        // TODO: Use method from `StreamService` which loads a collection of ids (when implemented) to prevent n+1.
        // Track https://github.com/Graylog2/graylog2-server/issues/4897 for progress.
        Set<Stream> streams = new HashSet<>();
        for (String streamId : streamIds) {
            try {
                Stream load = streamService.load(streamId);
                streams.add(load);
            } catch (NotFoundException e) {
                LOG.debug("Failed to load stream <{}>", streamId);
            }
        }
        return streams;
    }

    /**
     * Substitute query string parameters using ESQueryDecorators.
     */
    private String decorateQuery(Set<Parameter> queryParameters, TimeRange timeRange, String queryString) {
        // TODO
        // We need to create a dummy SearchJob and a Query to use the decorator API.
        // Maybe the decorate call could be refactored to make this easier.
        org.graylog.plugins.views.search.Search search = org.graylog.plugins.views.search.Search.builder()
                .parameters(ImmutableSet.copyOf(queryParameters))
                .build();
        SearchJob searchJob = new SearchJob("1234", search, "events backend");
        Query dummyQuery = Query.builder()
                .id("123")
                .timerange(timeRange)
                .query(ElasticsearchQueryString.builder().queryString(queryString).build())
                .build();
        return esQueryDecorators.decorate(queryString, searchJob, dummyQuery, ImmutableSet.of());
    }

    /**
     * Callback that receives message batches from {@link #scrollQuery(String, Set, Set, TimeRange, int, ScrollCallback)}.
     */
    public interface ScrollCallback {
        /**
         * This will be called with message batches from a scroll query. To stop the scroll query, the
         * {@code continueScrolling} boolean can be set to {@code false}.
         *
         * @param messages          the message batch
         * @param continueScrolling the boolean that can be set to {@code false} to stop the scroll query
         */
        void call(List<ResultMessage> messages, AtomicBoolean continueScrolling) throws EventProcessorException;
    }

    @VisibleForTesting
    static String buildStreamFilter(Set<String> streams) {
        checkArgument(streams != null, "streams parameter cannot be null");
        checkArgument(!streams.isEmpty(), "streams parameter cannot be empty");

        final String streamFilter = streams.stream()
                .map(String::trim)
                .map(stream -> String.format(Locale.ENGLISH, "streams:%s", stream))
                .collect(Collectors.joining(" OR "));
        return "(" + streamFilter + ")";
    }

    @AutoValue
    public static abstract class Result {
        public abstract List<ResultMessage> results();

        public abstract long resultsCount();

        public abstract long duration();

        public abstract Set<String> usedIndexNames();

        public abstract String executedQuery();

        public static Builder builder() {
            return new AutoValue_MoreSearch_Result.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder results(List<ResultMessage> results);

            public abstract Builder resultsCount(long resultsCount);

            public abstract Builder duration(long duration);

            public abstract Builder usedIndexNames(Set<String> usedIndexNames);

            public abstract Builder executedQuery(String executedQuery);

            public abstract Result build();
        }
    }
}
