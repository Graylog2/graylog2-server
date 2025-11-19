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
package org.graylog2.indexer.indexset.basic;

import org.graylog2.indexer.template.IndexTemplateConfig;

public interface BasicIndexSet {

    /**
     * The title for this index set.
     *
     * @return index set title
     */
    String title();

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
     * Returns an array with all managed indices in this index set.
     * <p>
     * Example: {@code ["graylog_0", "graylog_1", "graylog_2"]}
     *
     * @return array of index names
     */
    String[] getManagedIndices();

    /**
     * Checks if the given index name is part of this index set.
     *
     * @param index index name to check
     * @return true if part of index set, false if not
     */
    boolean isManagedIndex(String index);

    /**
     * The index template configuration for this index set.
     *
     * @return index template configuration
     */
    IndexTemplateConfig indexTemplateConfig();
}
