package org.graylog2.indexer;

import org.graylog2.indexer.indexset.BasicIndexSetConfig;

public interface BasicIndexSet {

    /**
     * Returns the index wildcard for this index set.
     * <p>
     * This can be used in Elasticsearch queries to match all managed indices in this index set.
     * <p>
     * Example: {@code "graylog_*"}
     *
     * @return the index wildcard
     */
    String getIndexWildcard();

    /**
     * Returns the write index alias name for this index set.
     * <p>
     * The write index alias always points to the newest index.
     * <p>
     * Example: {@code "graylog_deflector"}
     *
     * @return the write index alias name
     */
    String getWriteIndexAlias();

    BasicIndexSetConfig getBasicConfig();
}
