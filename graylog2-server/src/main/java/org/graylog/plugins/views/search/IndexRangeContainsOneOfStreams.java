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
package org.graylog.plugins.views.search;

import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.streams.Stream;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IndexRangeContainsOneOfStreams implements Predicate<IndexRange> {
    private final Set<IndexSet> validIndexSets;
    private final Set<String> validStreamIds;

    public IndexRangeContainsOneOfStreams(Set<Stream> validStreams) {
        this.validStreamIds = validStreams.stream().map(Stream::getId).collect(Collectors.toSet());
        this.validIndexSets = validStreams.stream().map(Stream::getIndexSet).collect(Collectors.toSet());
    }

    IndexRangeContainsOneOfStreams(Stream... validStreams) {
        this(ImmutableSet.copyOf(validStreams));
    }

    @Override
    public boolean test(IndexRange indexRange) {
        if (validIndexSets.isEmpty() && validStreamIds.isEmpty()) {
            return false;
        }
        // If index range is incomplete, check the prefix against the valid index sets.
        if (indexRange.streamIds() == null) {
            return validIndexSets.stream().anyMatch(indexSet -> indexSet.isManagedIndex(indexRange.indexName()));
        }
        // Otherwise check if the index range contains any of the valid stream ids.
        return !Collections.disjoint(indexRange.streamIds(), validStreamIds);
    }
}
