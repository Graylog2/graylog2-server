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

import org.joda.time.DateTime;

import java.util.Map;

/**
 * Counts indexing failures per target index. The default reads {@link IndexFailureService}; plugins that store
 * indexing failures elsewhere replace it.
 */
public interface IndexFailureCounter {
    /**
     * @return the number of indexing failures since the given time, keyed by the index the failed message targeted
     */
    Map<String, Long> countByIndexSince(DateTime since);
}
