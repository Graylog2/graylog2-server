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
package org.graylog2.rest.resources.search;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class SearchResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    protected static final String DEFAULT_SCROLL_BATCH_SIZE = "500";

    protected final Searches searches;
    private final ClusterConfigService clusterConfigService;
    private final DecoratorProcessor decoratorProcessor;
    private final SearchExecutor searchExecutor;

    public SearchResource(Searches searches,
                          ClusterConfigService clusterConfigService,
                          DecoratorProcessor decoratorProcessor,
                          SearchExecutor searchExecutor) {
        this.searches = searches;
        this.clusterConfigService = clusterConfigService;
        this.decoratorProcessor = decoratorProcessor;
        this.searchExecutor = searchExecutor;
    }

    protected SearchResponse search(String query, int limit, String filter, boolean decorate, SearchUser searchUser, List<String> fieldList, Sort sorting, TimeRange timeRange) {
        final Search search = createSearch(query, limit, filter, fieldList, sorting, timeRange);

        final Optional<String> streamId = Searches.extractStreamId(filter);

        final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());

        return extractSearchResponse(searchJob, query, decorate, fieldList, timeRange, streamId);
    }

    protected List<String> parseFields(String fields) {
        if (isNullOrEmpty(fields)) {
            LOG.warn("Missing fields parameter. Returning HTTP 400");
            throw new BadRequestException("Missing required parameter `fields`");
        }
        return parseOptionalFields(fields);
    }

    protected List<String> parseOptionalFields(String fields) {
        if (isNullOrEmpty(fields)) {
            return null;
        }

        final Iterable<String> split = Splitter.on(',').omitEmptyStrings().trimResults().split(fields);
        final ArrayList<String> fieldList = Lists.newArrayList(Message.FIELD_TIMESTAMP);

        // skip the mandatory field timestamp
        for (String field : split) {
            if (Message.FIELD_TIMESTAMP.equals(field)) {
                continue;
            }
            fieldList.add(field);
        }

        return fieldList;
    }

    protected SearchResponse buildSearchResponse(SearchResult sr,
                                                 org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange,
                                                 boolean decorate,
                                                 Optional<String> streamId) {
        final SearchResponse result = SearchResponse.create(sr.getOriginalQuery(),
            sr.getBuiltQuery(),
            indexRangeListToValueList(sr.getUsedIndices()),
            resultMessageListtoValueList(sr.getResults()),
            sr.getFields(),
            sr.tookMs(),
            sr.getTotalResults(),
            timeRange.getFrom(),
            timeRange.getTo());

        return decorate ? decoratorProcessor.decorate(result, streamId) : result;
    }

    protected SearchResponse buildSearchResponse(String query, MessageList.Result results, List<String> fieldList, long tookMs, TimeRange timeRange, boolean decorate, Optional<String> streamId) {
        final SearchResponse result = SearchResponse.create(query,
                query,
                Collections.emptySet(),
                results.messages(),
                fieldList == null ? Collections.emptySet() : ImmutableSet.copyOf(fieldList),
                tookMs,
                results.totalResults(),
                timeRange.getFrom(),
                timeRange.getTo());

        return decorate ? decoratorProcessor.decorate(result, streamId) : result;
    }

    protected Set<IndexRangeSummary> indexRangeListToValueList(Set<IndexRange> indexRanges) {
        final Set<IndexRangeSummary> result = Sets.newHashSetWithExpectedSize(indexRanges.size());

        for (IndexRange indexRange : indexRanges) {
            result.add(IndexRangeSummary.create(
                indexRange.indexName(),
                indexRange.begin(),
                indexRange.end(),
                indexRange.calculatedAt(),
                indexRange.calculationDuration()));
        }

        return result;
    }

    protected List<ResultMessageSummary> resultMessageListtoValueList(List<ResultMessage> resultMessages) {
        return resultMessages.stream()
            // TODO module merge: migrate to resultMessage.getMessage() instead of Map<String, Object> via getFields()
            .map((resultMessage) -> ResultMessageSummary.create(resultMessage.highlightRanges, resultMessage.getMessage().getFields(), resultMessage.getIndex()))
            .collect(Collectors.toList());
    }

    protected Sorting buildSorting(String sort) {
        if (isNullOrEmpty(sort)) {
            return Sorting.DEFAULT;
        }

        try {
            return Sorting.fromApiParam(sort);
        } catch (Exception e) {
            LOG.error("Falling back to default sorting.", e);
            return Sorting.DEFAULT;
        }
    }

    protected Sort buildSortOrder(String sort) {
        if (isNullOrEmpty(sort)) {
            return Sort.create("timestamp", Sort.Order.DESC);
        }

        if (!sort.contains(":")) {
            throw new IllegalArgumentException("Invalid sorting parameter: " + sort);
        }

        String[] parts = sort.split(":");

        return Sort.create(parts[0], Sort.Order.valueOf(parts[1].toUpperCase(Locale.ENGLISH)));
    }

    protected Search createSearch(String queryString, int limit, String filter, List<String> fieldList, Sort sorting, TimeRange timeRange) {
        final SearchType searchType = createMessageList(sorting, limit, fieldList);

        final Query query = Query.builder()
                .query(ElasticsearchQueryString.of(queryString))
                .filter(QueryStringFilter.builder()
                        .query(Strings.isNullOrEmpty(filter) ? "*" : filter)
                        .build())
                .timerange(timeRange)
                .searchTypes(Collections.singleton(searchType))
                .build();
        return Search.Builder.create()
                .queries(ImmutableSet.of(query))
                .build();
    }

    private SearchType createMessageList(Sort sorting, int limit, List<String> fieldList) {
        MessageList.Builder messageListBuilder = MessageList.builder()
                .sort(Collections.singletonList(sorting));
        messageListBuilder = limit > 0 ? messageListBuilder.limit(limit) : messageListBuilder;
        messageListBuilder = fieldList != null && !fieldList.isEmpty() ? messageListBuilder.fields(fieldList) : messageListBuilder;
        return messageListBuilder.build();
    }

    protected SearchResponse extractSearchResponse(SearchJob searchJob, String query, boolean decorate, List<String> fieldList, TimeRange timeRange, Optional<String> streamId) {
        final QueryResult queryResult = searchJob.results()
                .values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing query result"));
        final MessageList.Result result = queryResult.searchTypes()
                .values()
                .stream()
                .findFirst()
                .map(searchTypeResult -> (MessageList.Result)searchTypeResult)
                .orElseThrow(() -> new IllegalStateException("Missing search type result!"));

        final long tookMs = queryResult.executionStats().duration();

        return buildSearchResponse(query, result, fieldList, tookMs, timeRange, decorate, streamId);
    }

    protected ChunkedOutput<ScrollResult.ScrollChunk> buildChunkedOutput(final ScrollResult scroll) {
        final ChunkedOutput<ScrollResult.ScrollChunk> output = new ChunkedOutput<>(ScrollResult.ScrollChunk.class);

        LOG.debug("[{}] Scroll result contains a total of {} messages", scroll.getQueryHash(), scroll.totalHits());
        Runnable scrollIterationAction = createScrollChunkProducer(scroll, output);
        // TODO use a shared executor for async responses here instead of a single thread that's not limited
        new Thread(scrollIterationAction).start();
        return output;
    }

    public void checkSearchPermission(String filter, String searchPermission) {
        if (isNullOrEmpty(filter) || "*".equals(filter)) {
            checkPermission(searchPermission);
        } else {
            if (!filter.startsWith("streams:")) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            String[] parts = filter.split(":");
            if (parts.length <= 1) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            String streamList = parts[1];
            String[] streams = streamList.split(",");
            if (streams.length == 0) {
                throw new ForbiddenException("Not allowed to search with filter: [" + filter + "]");
            }

            for (String streamId : streams) {
                if (!isPermitted(RestPermissions.STREAMS_READ, streamId)) {
                    final String msg = "Not allowed to search with filter: [" + filter + "]. (Forbidden stream: " + streamId + ")";
                    LOG.warn(msg);
                    throw new ForbiddenException(msg);
                }
            }
        }
    }

    protected Runnable createScrollChunkProducer(final ScrollResult scroll,
                                                 final ChunkedOutput<ScrollResult.ScrollChunk> output) {
        return () -> {
            try {
                ScrollResult.ScrollChunk chunk = scroll.nextChunk();
                while (chunk != null) {
                    LOG.debug("[{}] Writing scroll chunk with {} messages",
                        scroll.getQueryHash(),
                        chunk.getMessages().size());
                    if (output.isClosed()) {
                        LOG.debug("[{}] Client connection is closed, client disconnected. Aborting scroll.",
                            scroll.getQueryHash());
                        scroll.cancel();
                        return;
                    }
                    output.write(chunk);
                    chunk = scroll.nextChunk();
                }
                LOG.debug("[{}] Reached end of scroll result.", scroll.getQueryHash());
                output.close();
            } catch (IOException e) {
                LOG.warn("[{}] Could not close chunked output stream for query scroll.", scroll.getQueryHash());
            }
        };
    }

    protected org.graylog2.plugin.indexer.searches.timeranges.TimeRange restrictTimeRange(final org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange) {
        final DateTime originalFrom = timeRange.getFrom();
        final DateTime to = timeRange.getTo();
        final DateTime from;

        final SearchesClusterConfig config = clusterConfigService.get(SearchesClusterConfig.class);

        if (config == null || Period.ZERO.equals(config.queryTimeRangeLimit())) {
            from = originalFrom;
        } else {
            final DateTime limitedFrom = to.minus(config.queryTimeRangeLimit());
            from = limitedFrom.isAfter(originalFrom) ? limitedFrom : originalFrom;
        }

        return AbsoluteRange.create(from, to);
    }
}
