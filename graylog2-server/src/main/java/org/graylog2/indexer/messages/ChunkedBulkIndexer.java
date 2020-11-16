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
import java.util.Collections;
import java.util.List;

public class ChunkedBulkIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkedBulkIndexer.class);

    public interface BulkIndex {
        List<Messages.IndexingError> apply(Chunk chunk) throws ChunkedBulkIndexer.EntityTooLargeException, IOException;
    }

    public List<Messages.IndexingError> index(List<IndexingRequest> messageList, BulkIndex bulkIndex) throws IOException {
        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        int chunkSize = messageList.size();
        int offset = 0;
        for (;;) {
            try {
                return bulkIndex.apply(new Chunk(messageList, offset, chunkSize));
            } catch (ChunkedBulkIndexer.EntityTooLargeException e) {
                LOG.warn("Bulk index failed with 'Request Entity Too Large' error. Retrying by splitting up batch size <{}>.", chunkSize);
                if (chunkSize == messageList.size()) {
                    LOG.warn("Consider lowering the \"output_batch_size\" setting.");
                }
                offset += e.indexedSuccessfully;
                chunkSize /= 2;
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
        public final List<Messages.IndexingError> failedItems;

        public EntityTooLargeException(int indexedSuccessfully, List<Messages.IndexingError> failedItems)  {
            this.indexedSuccessfully = indexedSuccessfully;
            this.failedItems = failedItems;
        }
    }
}
