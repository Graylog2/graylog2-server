/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch6;

import com.google.common.base.Stopwatch;
import io.searchbox.core.Search;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.params.Parameters;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.search.MoreSearch;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.common.xcontent.ToXContent;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class MoreSearchAdapterES6 implements MoreSearchAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(MoreSearchAdapterES6.class);
    private final MultiSearch multiSearch;
    private final Scroll scroll;
    private final Boolean allowLeadingWildcard;
    private final SortOrderMapper sortOrderMapper;

    @Inject
    public MoreSearchAdapterES6(@Named("allow_leading_wildcard_searches") Boolean allowLeadingWildcard, MultiSearch multiSearch, Scroll scroll, SortOrderMapper sortOrderMapper) {
        this.allowLeadingWildcard = allowLeadingWildcard;
        this.multiSearch = multiSearch;
        this.scroll = scroll;
        this.sortOrderMapper = sortOrderMapper;
    }

    @Override
    public MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices, Sorting sorting, int page, int perPage, Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams) {
        final QueryBuilder query = (queryString.isEmpty() || queryString.equals("*")) ?
                matchAllQuery() :
                queryStringQuery(queryString).allowLeadingWildcard(allowLeadingWildcard);

        final BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(termsQuery(EventDto.FIELD_STREAMS, eventStreams))
                .filter(requireNonNull(TimeRangeQueryFactory.create(timerange)));

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
                .sort(sorting.getField(), sortOrderMapper.fromSorting(sorting));

        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .addType(IndexMapping.TYPE_MESSAGE)
                .addIndex(affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices)
                .allowNoIndices(false)
                .ignoreUnavailable(false);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Query:\n{}", searchSourceBuilder.toString(new ToXContent.MapParams(Collections.singletonMap("pretty", "true"))));
            LOG.debug("Execute search: {}", searchBuilder.build().toString());
        }

        final io.searchbox.core.SearchResult searchResult = multiSearch.wrap(searchBuilder.build(), () -> "Unable to perform search query");

        @SuppressWarnings("unchecked") final List<ResultMessage> hits = searchResult.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source, hit.highlight))
                .collect(Collectors.toList());

        return MoreSearch.Result.builder()
                .results(hits)
                .resultsCount(searchResult.getTotal())
                .duration(multiSearch.tookMsFromSearchResult(searchResult))
                .usedIndexNames(affectedIndices)
                .executedQuery(searchSourceBuilder.toString())
                .build();
    }

    @Override
    public void scrollEvents(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams, String scrollTime, int batchSize, ScrollEventsCallback resultCallback) throws EventProcessorException {
        final QueryBuilder query = (queryString.trim().isEmpty() || queryString.trim().equals("*")) ?
                matchAllQuery() :
                queryStringQuery(queryString).allowLeadingWildcard(allowLeadingWildcard);

        final BoolQueryBuilder filter = boolQuery()
                .filter(query)
                .filter(requireNonNull(TimeRangeQueryFactory.create(timeRange)));

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

        final ScrollResult scrollResult = scroll.scroll(searchBuilder.build(),
                () -> "Unable to scroll indices.",
                searchSourceBuilder.toString(),
                scrollTime,
                Collections.emptyList());
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
}
