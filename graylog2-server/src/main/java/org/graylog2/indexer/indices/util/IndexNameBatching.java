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
package org.graylog2.indexer.indices.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility for partitioning index names into batches that fit within URL length limits.
 * <p>
 * OpenSearch/Elasticsearch have a default HTTP line limit of 4096 bytes. When index names are
 * concatenated into a URL path (e.g., {@code /index1,index2,.../_stats}), exceeding this limit
 * causes a {@code too_long_http_line_exception}. This utility splits index names into batches
 * where each batch's comma-joined string stays within a safe threshold.
 */
public final class IndexNameBatching {

    /**
     * Maximum length (in characters) for the comma-joined index names portion of a URL.
     * Set below the 4096-byte HTTP line limit to leave room for the rest of the URL
     * (endpoint path, query parameters, etc.).
     */
    public static final int MAX_INDICES_URL_LENGTH = 3000;

    private IndexNameBatching() {
    }

    /**
     * Partitions index names into batches where each batch's comma-joined string does not
     * exceed {@code maxJoinedLength}. A single index name that exceeds the limit is placed
     * into its own batch (never dropped).
     *
     * @param items           the index names to partition
     * @param maxJoinedLength the maximum length of the comma-joined string for each batch
     * @return a list of batches; each batch is a non-empty list of index names
     */
    public static List<List<String>> partitionByJoinedLength(final Collection<String> items, final int maxJoinedLength) {
        final List<List<String>> batches = new ArrayList<>();
        if (items.isEmpty()) {
            return batches;
        }

        List<String> currentBatch = new ArrayList<>();
        int currentLength = 0;

        for (final String item : items) {
            final int itemLength = item.length();
            final int lengthWithSeparator = currentBatch.isEmpty() ? itemLength : currentLength + 1 + itemLength;

            if (!currentBatch.isEmpty() && lengthWithSeparator > maxJoinedLength) {
                batches.add(currentBatch);
                currentBatch = new ArrayList<>();
                currentLength = 0;
            }

            currentBatch.add(item);
            currentLength = currentBatch.size() == 1 ? itemLength : currentLength + 1 + itemLength;
        }

        batches.add(currentBatch);

        return batches;
    }
}
