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
package org.graylog2.indexer.messages;

import org.graylog2.indexer.ElasticsearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ChunkedBulkIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkedBulkIndexer.class);
    private static final RetryWait retryWait = new RetryWait(100);

    public interface BulkIndex {
        IndexingResults apply(Chunk chunk) throws ChunkedBulkIndexer.EntityTooLargeException, IOException;
    }

    public IndexingResults index(List<IndexingRequest> messageList, BulkIndex bulkIndex) throws IOException {
        if (messageList.isEmpty()) {
            return IndexingResults.empty();
        }

        int chunkSize = messageList.size();
        int offset = 0;
        IndexingResults.Builder accumulatedResults = IndexingResults.Builder.create();
        int attempt = 0;
        for (; ; ) {
            try {
                var results = bulkIndex.apply(new Chunk(messageList, offset, chunkSize));
                accumulatedResults.addResults(results);
                return accumulatedResults.build();
            } catch (EntityTooLargeException e) {
                final var retryForever = e instanceof CircuitBreakerException cbe && cbe.isTransient();
                LOG.warn("Bulk index failed with '{}' error. Retrying by splitting up batch size <{}>.", e.description(), chunkSize);
                if (chunkSize == messageList.size()) {
                    LOG.warn("Consider lowering the \"output_batch_size\" setting. Or resizing your Search cluster");
                }
                offset += e.indexedSuccessfully;
                chunkSize = Math.max(chunkSize / 2, retryForever ? 1 : 0);
                accumulatedResults.addResults(e.previousResults);

                if (retryForever && chunkSize == 1) {
                    retryWait.waitBeforeRetrying(attempt++);
                }
            }
            if (chunkSize == 0) {
                throw new ElasticsearchException("Bulk index cannot split output batch any further.");
            }
        }
    }

    public static class Chunk {
        public final List<IndexingRequest> requests;
        public final int offset;
        public final int size;

        Chunk(List<IndexingRequest> requests, int offset, int size) {
            this.requests = requests;
            this.offset = offset;
            this.size = size;
        }
    }

    public static class EntityTooLargeException extends Exception {
        public final int indexedSuccessfully;
        public final IndexingResults previousResults;

        String description() {
            return "Request Entity Too Large";
        }

        public EntityTooLargeException(int indexedSuccessfully, IndexingResults previousResults) {
            this.indexedSuccessfully = indexedSuccessfully;
            this.previousResults = previousResults;
        }
    }

    public static class TooManyRequestsException extends EntityTooLargeException {
        String description() {
            return "Too many requests";
        }

        public TooManyRequestsException(int indexedSuccessfully, IndexingResults previousResults) {
            super(indexedSuccessfully, previousResults);
        }
    }

    public static class CircuitBreakerException extends EntityTooLargeException {
        public enum Durability {
            Transient,
            Permanent
        }

        private final Durability durability;

        private boolean isTransient() {
            return durability == Durability.Transient;
        }

        String description() {
            return "Data too large";
        }

        public CircuitBreakerException(int indexedSuccessfully, IndexingResults previousResults, Durability durability) {
            super(indexedSuccessfully, previousResults);
            this.durability = durability;
        }
    }
}
