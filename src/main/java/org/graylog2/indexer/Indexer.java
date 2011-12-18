package org.graylog2.indexer;

import org.graylog2.messagehandlers.gelf.GELFMessage;

import java.util.List;

/**
 */
public interface Indexer {
    boolean bulkIndex(List<GELFMessage> messages);

    boolean deleteMessagesByTimeRange(int to);
}
