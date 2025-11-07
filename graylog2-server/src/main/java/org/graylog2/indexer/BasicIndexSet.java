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
package org.graylog2.indexer;

import org.graylog2.indexer.indexset.BasicIndexSetConfig;

public interface BasicIndexSet<T extends BasicIndexSetConfig> {

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

    T getConfig();

    /**
     * Returns the newest index.
     * <p>
     * Example: {@code "graylog_42"}
     *
     * @return the newest index
     * @throws NoTargetIndexException if there are no indices in this index set yet
     */
    String getNewestIndex() throws NoTargetIndexException;
}
