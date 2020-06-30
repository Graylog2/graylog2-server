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
package org.graylog2.indexer.messages;

import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class ChunkedBulkIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkedBulkIndexer.class);

    public interface BulkIndex {
        List<IndexFailure> apply(Chunk chunk) throws ChunkedBulkIndexer.EntityTooLargeException;
    }

    public List<IndexFailure> index(List<IndexingRequest> messageList, BulkIndex bulkIndex) {
        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        int chunkSize = messageList.size();
        int offset = 0;
        for (;;) {
            try {
                final List<IndexFailure> failures = bulkIndex.apply(new Chunk(messageList, offset, chunkSize));
                return failures;
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
        public final List<IndexFailure> failedItems;

        public EntityTooLargeException(int indexedSuccessfully, List<IndexFailure> failedItems)  {
            this.indexedSuccessfully = indexedSuccessfully;
            this.failedItems = failedItems;
        }
    }
}
