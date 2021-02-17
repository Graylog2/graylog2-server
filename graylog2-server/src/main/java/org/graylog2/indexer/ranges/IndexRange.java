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
package org.graylog2.indexer.ranges;

import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.Set;

public interface IndexRange {
    String FIELD_TOOK_MS = "took_ms";
    String FIELD_CALCULATED_AT = "calculated_at";
    String FIELD_END = "end";
    String FIELD_BEGIN = "begin";
    String FIELD_INDEX_NAME = "index_name";
    String FIELD_STREAM_IDS = "stream_ids";
    Comparator<IndexRange> COMPARATOR = new IndexRangeComparator();

    String indexName();

    DateTime begin();

    DateTime end();

    DateTime calculatedAt();

    int calculationDuration();

    Set<String> streamIds();
}
