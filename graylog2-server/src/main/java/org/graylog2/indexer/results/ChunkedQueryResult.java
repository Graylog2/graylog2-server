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
package org.graylog2.indexer.results;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public abstract class ChunkedQueryResult<C, R> extends IndexQueryResult implements ChunkedResult {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedQueryResult.class);

    protected final C client;
    protected R initialResult;
    protected R lastSearchResponse;
    private final List<String> fields;
    private final long totalHits;
    private final String queryHash;
    private final int limit;
    private int chunkId = 0;
    private int resultCount = 0;
    private final long tookMs;


    public ChunkedQueryResult(C client,
                              R initialResult,
                              String query,
                              List<String> fields,
                              int limit) {
        super(query, null);
        this.tookMs = getTookMillisFromResponse(initialResult);
        this.client = client;
        this.totalHits = countTotalHits(initialResult);
        this.limit = limit;
        this.initialResult = initialResult;
        this.fields = fields;

        final Md5Hash md5Hash = new Md5Hash(getOriginalQuery());
        queryHash = md5Hash.toHex();
        LOG.debug("[{}] Starting {} request for query {}", queryHash, getChunkingMethodName(), getOriginalQuery());
    }

    @Override
    @Nullable
    public ResultChunk nextChunk() throws IOException {
        if (limitReached()) {
            LOG.debug("[{}] Reached limit for query {}", queryHash, getOriginalQuery());
            return null;
        }

        final R result = this.initialResult != null ? this.initialResult : nextSearchResult();
        this.lastSearchResponse = result;
        this.initialResult = null;

        final List<ResultMessage> resultMessages = result != null ? collectMessagesFromResult(result) : List.of();

        if (resultMessages.isEmpty()) {
            // chunking exhausted
            LOG.debug("[{}] Reached end of {} results for query {}", queryHash, getChunkingMethodName(), getOriginalQuery());
            return null;
        }

        final int remainingResultsForLimit = limit - resultCount;

        final List<ResultMessage> resultMessagesSlice = (limit != -1 && remainingResultsForLimit < resultMessages.size())
                ? resultMessages.subList(0, remainingResultsForLimit)
                : resultMessages;

        resultCount += resultMessagesSlice.size();

        return new ResultChunk(fields, chunkId++, resultMessagesSlice);
    }

    protected abstract List<ResultMessage> collectMessagesFromResult(R result);

    @Nullable
    protected abstract R nextSearchResult() throws IOException;

    protected abstract String getChunkingMethodName();

    protected abstract long countTotalHits(R response);

    protected abstract long getTookMillisFromResponse(R response);

    @Override
    public long tookMs() {
        return this.tookMs;
    }

    private boolean limitReached() {
        return limit != -1 && resultCount >= limit;
    }

    @Override
    public String getQueryHash() {
        return this.queryHash;
    }

    @Override
    public long totalHits() {
        return this.totalHits;
    }
}
