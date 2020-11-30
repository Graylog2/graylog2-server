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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.ClearScroll;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.graylog2.indexer.results.IndexQueryResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.jackson.TypeReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.graylog2.indexer.searches.ScrollCommand.NO_LIMIT;

public class ScrollResultES6 extends IndexQueryResult implements ScrollResult {
    private static final Logger LOG = LoggerFactory.getLogger(ScrollResult.class);
    private static final String DEFAULT_SCROLL = "1m";
    private static final String SCROLL_ID_FIELD = "_scroll_id";

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private SearchResult initialResult;
    private final String scroll;
    private final List<String> fields;
    private final String queryHash; // used in log output only
    private final long totalHits;

    private String scrollId;
    private final int limit;
    private int chunkId = 0;
    private int resultCount = 0;

    public interface Factory {
        ScrollResultES6 create(SearchResult initialResult, @Assisted("query") String query, @Assisted("scroll") String scroll,
                               List<String> fields, int limit);
    }

    @AssistedInject
    public ScrollResultES6(JestClient jestClient,
                           ObjectMapper objectMapper,
                           @Assisted SearchResult initialResult,
                           @Assisted("query") String query,
                           @Assisted List<String> fields,
                           @Assisted int limit) {
        this(jestClient, objectMapper, initialResult, query, DEFAULT_SCROLL, fields, limit);
    }

    @AssistedInject
    public ScrollResultES6(JestClient jestClient,
                           ObjectMapper objectMapper,
                           @Assisted SearchResult initialResult,
                           @Assisted("query") String query,
                           @Assisted("scroll") String scroll,
                           @Assisted List<String> fields,
                           @Assisted int limit) {
        super(query, null, initialResult.getJsonObject().path("took").asLong());
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.initialResult = initialResult;
        this.scroll = scroll;
        this.fields = fields;
        totalHits = initialResult.getTotal();
        scrollId = getScrollIdFromResult(initialResult);
        this.limit = limit;

        final Md5Hash md5Hash = new Md5Hash(getOriginalQuery());
        queryHash = md5Hash.toHex();

        LOG.debug("[{}] Starting scroll request for query {}", queryHash, getOriginalQuery());
    }

    @Override
    public ScrollChunk nextChunk() throws IOException {
        if (limit != NO_LIMIT && resultCount >= limit) {
            LOG.debug("[{}] Reached limit for query {}", queryHash, getOriginalQuery());
            return null;
        }

        final JestResult search;
        final List<ResultMessage> resultMessages;
        if (initialResult == null) {
            search = getNextScrollResult();
            resultMessages = StreamSupport.stream(search.getJsonObject().path("hits").path("hits").spliterator(), false)
                    .map(hit -> ResultMessage.parseFromSource(hit.path("_id").asText(),
                            hit.path("_index").asText(),
                            objectMapper.convertValue(hit.get("_source"), TypeReferences.MAP_STRING_OBJECT)))
                    .collect(Collectors.toList());
        } else {
            // make sure to return the initial resultMessages, see https://github.com/Graylog2/graylog2-server/issues/2126
            search = initialResult;
            resultMessages = initialResult.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>)hit.source))
                .collect(Collectors.toList());
            this.initialResult = null;
        }

        if (resultMessages.size() == 0) {
            // scroll exhausted
            LOG.debug("[{}] Reached end of scroll results for query {}", queryHash, getOriginalQuery());
            return null;
        }

        final int remainingResultsForLimit = limit - resultCount;

        final List<ResultMessage> resultMessagesSlice = (limit != NO_LIMIT && remainingResultsForLimit < resultMessages.size())
                ? resultMessages.subList(0, remainingResultsForLimit)
                : resultMessages;

        resultCount += resultMessagesSlice.size();

        LOG.debug("[{}][{}] New scroll id {}, number of resultMessages in chunk: {}", queryHash, chunkId,
                getScrollIdFromResult(search), resultMessagesSlice.size());
        scrollId = getScrollIdFromResult(search); // save the id for the next request.

        return new ScrollChunkES6(resultMessagesSlice, fields, chunkId++);
    }

    private String getScrollIdFromResult(JestResult result) {
        if (!result.getJsonObject().hasNonNull(SCROLL_ID_FIELD)) {
            throw new IllegalStateException("Unable to extract scroll id from search result!");
        }
        return result.getJsonObject().path(SCROLL_ID_FIELD).asText();
    }

    private JestResult getNextScrollResult() throws IOException {
        final SearchScroll.Builder searchBuilder = new SearchScroll.Builder(scrollId, scroll);
        return jestClient.execute(searchBuilder.build());
    }

    @Override
    public String getQueryHash() {
        return queryHash;
    }

    @Override
    public long totalHits() {
        return totalHits;
    }

    @Override
    public void cancel() throws IOException {
        final ClearScroll.Builder clearScrollBuilder = new ClearScroll.Builder().addScrollId(scrollId);
        final JestResult result = jestClient.execute(clearScrollBuilder.build());
        LOG.debug("[{}] clearScroll for query successful: {}", queryHash, result.isSucceeded());
    }

    static class ScrollChunkES6 implements ScrollResult.ScrollChunk {
        private final List<ResultMessage> resultMessages;
        private final List<String> fields;
        private final int chunkNumber;

        public ScrollChunkES6(List<ResultMessage> hits, List<String> fields, int chunkId) {
            this.resultMessages = hits;
            this.fields = fields;
            this.chunkNumber = chunkId;
        }

        public List<String> getFields() {
            return fields;
        }

        public int getChunkNumber() {
            return chunkNumber;
        }

        public List<ResultMessage> getMessages() {
            return resultMessages;
        }
    }
}
