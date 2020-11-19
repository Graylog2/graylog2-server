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
package org.graylog2.indexer.searches;

import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
import java.util.Set;

public interface SearchesAdapter {
    CountResult count(Set<String> affectedIndices, String query, TimeRange range, String filter);

    ScrollResult scroll(Set<String> indexWildcards, Sorting sorting, String filter, String query, TimeRange range, int limit, int offset, List<String> fields);
    ScrollResult scroll(Set<String> indexWildcards, Sorting sorting, String filter, String query, int batchSize);
    ScrollResult scroll(ScrollCommand scrollCommand);

    SearchResult search(Set<String> indices, Set<IndexRange> indexRanges, SearchesConfig config);

    FieldStatsResult fieldStats(String query, String filter, TimeRange range, Set<String> indices, String field, boolean includeCardinality, boolean includeStats, boolean includeCount);
}
