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

import com.google.common.base.Strings;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkedBulkIndexerTest {
    private final ChunkedBulkIndexer indexer = new ChunkedBulkIndexer();
    private final MessageFactory messageFactory = new TestMessageFactory();
    protected static final IndexSet indexSet = new MessagesTestIndexSet();
    private final List<IndexingRequest> indexingRequests = createMessageBatch(200, 1024);

    @Test
    void retriesIndexingIfDataTooLarge() throws IOException {
        final ChunkedBulkIndexer.BulkIndex bulkIndex = (indexed, previous, chunk) -> {
            if (chunk.size() > 100) {
                throw circuitBreakerException();
            } else {
                return success(chunk);
            }
        };
        final var result = indexer.index(indexingRequests, bulkIndex);
        assertThat(result.successes()).hasSize(indexingRequests.size());
    }

    @Test
    void continuesRetryingForeverForDataTooLarge() throws IOException {
        final ChunkedBulkIndexer.BulkIndex bulkIndex = new ChunkedBulkIndexer.BulkIndex() {
            private int attempt = 0;

            @Override
            public ChunkedBulkIndexer.BulkIndexResult apply(int indexedSuccessfully, IndexingResults previousResults, List<IndexingRequest> chunk) throws ChunkedBulkIndexer.EntityTooLargeException, IOException {
                final var cbe = new ChunkedBulkIndexer.CircuitBreakerException(indexedSuccessfully, previousResults, ChunkedBulkIndexer.CircuitBreakerException.Durability.Transient);
                if (attempt < 5 && chunk.size() > 1) {
                    throw cbe;
                } else {
                    attempt += 1;
                    if (attempt < 5) {
                        throw cbe;
                    }
                    return success(chunk);
                }
            }
        };
        final var result = indexer.index(indexingRequests, bulkIndex);
        assertThat(result.successes()).hasSize(indexingRequests.size());
    }

    @Test
    void doesNotRetryPermanentCircuitBreakerExceptions() {
        final ChunkedBulkIndexer.BulkIndex bulkIndex = (indexed, previous, chunk) -> {
            throw circuitBreakerException(ChunkedBulkIndexer.CircuitBreakerException.Durability.Permanent);
        };
        assertThatThrownBy(() -> indexer.index(indexingRequests, bulkIndex))
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageContaining("Bulk index cannot split output batch any further.");
    }

    private ChunkedBulkIndexer.BulkIndexResult success(List<IndexingRequest> requests) {
        final var results = requests.stream()
                .map(request -> IndexingSuccess.create(request.message(), request.indexSet().getNewestIndex()))
                .toList();
        return new ChunkedBulkIndexer.BulkIndexResult(IndexingResults.create(results, List.of()), () -> "", requests.size());
    }

    private ChunkedBulkIndexer.CircuitBreakerException circuitBreakerException() {
        return circuitBreakerException(ChunkedBulkIndexer.CircuitBreakerException.Durability.Transient);
    }

    private ChunkedBulkIndexer.CircuitBreakerException circuitBreakerException(ChunkedBulkIndexer.CircuitBreakerException.Durability durability) {
        return new ChunkedBulkIndexer.CircuitBreakerException(0, IndexingResults.empty(), durability);
    }

    private List<IndexingRequest> createMessageBatch(int size, int count) {
        final List<IndexingRequest> messageList = new ArrayList<>();

        final String message = Strings.repeat("A", size);
        for (int i = 0; i < count; i++) {
            messageList.add(IndexingRequest.create(indexSet, messageFactory.createMessage(i + message, "source", now())));
        }
        return messageList;
    }

    private DateTime now() {
        return DateTime.now(DateTimeZone.UTC);
    }
}
