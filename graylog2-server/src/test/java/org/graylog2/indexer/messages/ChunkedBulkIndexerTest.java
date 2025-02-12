package org.graylog2.indexer.messages;

import com.google.common.base.Strings;
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

class ChunkedBulkIndexerTest {
    private final ChunkedBulkIndexer indexer = new ChunkedBulkIndexer();
    private final MessageFactory messageFactory = new TestMessageFactory();
    protected static final IndexSet indexSet = new MessagesTestIndexSet();
    private final List<IndexingRequest> indexingRequests = createMessageBatch(200, 1024);

    @Test
    void retriesIndexingIfDataTooLarge() throws IOException {
        final ChunkedBulkIndexer.BulkIndex bulkIndex = (chunk) -> {
            if (chunk.size > 100) {
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
            public IndexingResults apply(ChunkedBulkIndexer.Chunk chunk) throws ChunkedBulkIndexer.EntityTooLargeException, IOException {
                if (chunk.size > 1) {
                    throw circuitBreakerException();
                } else {
                    attempt += 1;
                    if (attempt < 5) {
                        throw circuitBreakerException();
                    }
                    return success(chunk);
                }
            }
        };
        final var result = indexer.index(indexingRequests, bulkIndex);
        assertThat(result.successes()).hasSize(indexingRequests.size());
    }

    private IndexingResults success(ChunkedBulkIndexer.Chunk chunk) {
        final var results = chunk.requests.stream()
                .map(request -> IndexingSuccess.create(request.message(), request.indexSet().getNewestIndex()))
                .toList();
        return IndexingResults.create(results, List.of());
    }

    private ChunkedBulkIndexer.CircuitBreakerException circuitBreakerException() {
        return new ChunkedBulkIndexer.CircuitBreakerException(0, IndexingResults.empty());
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
