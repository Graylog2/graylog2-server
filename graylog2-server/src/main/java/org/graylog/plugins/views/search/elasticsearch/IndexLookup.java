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
package org.graylog.plugins.views.search.elasticsearch;

import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Collection;
import java.util.Set;

public interface IndexLookup {
    Set<String> indexNamesForStreamsInTimeRange(Collection<String> streamIds,
                                                TimeRange timeRange);

    Set<IndexRange> indexRangesForStreamsInTimeRange(Collection<String> streamIds,
                                                     TimeRange timeRange);
}
