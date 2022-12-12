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
package org.graylog.storage.opensearch2;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Streams;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.search.MoreSearch;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.IndicesOptions;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.ToXContent;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.results.ResultChunk;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.ChunkCommand;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.matchAllQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.queryStringQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.termsQuery;

public class MoreSearchAdapterOS2 implements MoreSearchAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(MoreSearchAdapterOS2.class);
    public static final IndicesOptions INDICES_OPTIONS = IndicesOptions.fromOptions(false, false, true, false);
    private final OpenSearchClient client;
    private final Boolean allowLeadingWildcard;
    private final SortOrderMapper sortOrderMapper;
    private final MultiChunkResultRetriever multiChunkResultRetriever;

    @Inject
    public MoreSearchAdapterOS2(OpenSearchClient client,
                                @Named("allow_leading_wildcard_searches") Boolean allowLeadingWildcard,
                                SortOrderMapper sortOrderMapper,
                                MultiChunkResultRetriever multiChunkResultRetriever) {
        this.client = client;
        this.allowLeadingWildcard = allowLeadingWildcard;
        this.sortOrderMapper = sortOrderMapper;
        this.multiChunkResultRetriever = multiChunkResultRetriever;
    }

    @Override
    public MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices,
                                         Sorting sorting, int page, int perPage, Set<String> eventStreams,
                                         String filterString, Set<String> forbiddenSourceStreams) {
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
                .sort(sorting.getField(), sortOrderMapper.fromSorting(sorting))
                .trackTotalHits(true);

        final Set<String> indices = affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices;
        final SearchRequest searchRequest = new SearchRequest(indices.toArray(new String[0]))
                .source(searchSourceBuilder)
                .indicesOptions(INDICES_OPTIONS);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Query:\n{}", searchSourceBuilder.toString(new ToXContent.MapParams(Collections.singletonMap("pretty", "true"))));
            LOG.debug("Execute search: {}", searchRequest.toString());
        }

        final SearchResponse searchResult = client.search(searchRequest, "Unable to perform search query");

        final List<ResultMessage> hits = Streams.stream(searchResult.getHits())
                .map(ResultMessageFactory::fromSearchHit)
                .collect(Collectors.toList());

        final long total = searchResult.getHits().getTotalHits().value;

        return MoreSearch.Result.builder()
                .results(hits)
                .resultsCount(total)
                .duration(searchResult.getTook().getMillis())
                .usedIndexNames(affectedIndices)
                .executedQuery(searchSourceBuilder.toString())
                .build();
    }

    @Override
    public void scrollEvents(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams, int batchSize, ScrollEventsCallback resultCallback) throws EventProcessorException {
        final ChunkCommand chunkCommand = buildScrollCommand(queryString, timeRange, affectedIndices, streams, batchSize);

        final ChunkedResult chunkedResult = multiChunkResultRetriever.retrieveChunkedResult(chunkCommand);

        final AtomicBoolean continueScrolling = new AtomicBoolean(true);

        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            ResultChunk resultChunk = chunkedResult.nextChunk();
            while (continueScrolling.get() && resultChunk != null) {
                final List<ResultMessage> messages = resultChunk.messages();

                LOG.debug("Passing <{}> messages to callback", messages.size());
                resultCallback.accept(Collections.unmodifiableList(messages), continueScrolling);

                // Stop if the resultCallback told us to stop
                if (!continueScrolling.get()) {
                    break;
                }

                resultChunk = chunkedResult.nextChunk();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                // Tell Elasticsearch that we are done with the scroll so it can release resources as soon as possible
                // instead of waiting for the scroll timeout to kick in.
                chunkedResult.cancel();
            } catch (Exception ignored) {
            }
            LOG.debug("Scrolling done - took {} ms", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private ChunkCommand buildScrollCommand(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams, int batchSize) {
        ChunkCommand.Builder commandBuilder = ChunkCommand.builder()
                .query(queryString)
                .range(timeRange)
                .indices(affectedIndices)
                .batchSize(batchSize)
                // For correlation need the oldest messages to come in first
                .sorting(new Sorting(Message.FIELD_TIMESTAMP, Sorting.Direction.ASC));

        if (!streams.isEmpty()) {
            commandBuilder = commandBuilder.streams(streams);
        }

        return commandBuilder
                .build();
    }
}
