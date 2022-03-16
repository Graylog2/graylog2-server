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
package org.graylog2.indexer.indices.blocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IndicesBlockStatus {

    private Map<String, Collection<String>> blocksPerIndex = new HashMap<>();

    public void addIndexBlocks(final String indexName, final Collection<String> blockTypes) {
        blocksPerIndex.put(indexName, blockTypes);
    }

    public Collection<String> getIndexBlocks(final String indexName) {
        return blocksPerIndex.get(indexName);
    }

    public int countBlockedIndices() {
        return blocksPerIndex.size();
    }

    public Set<String> getBlockedIndices() {
        return blocksPerIndex.keySet();
    }

    public List<String[]> toBlockDetails() {
        return blocksPerIndex.entrySet()
                .stream()
                .map(entry -> new String[]{entry.getKey(), String.join(", ", entry.getValue())})
                .collect(Collectors.toList());
    }
}
