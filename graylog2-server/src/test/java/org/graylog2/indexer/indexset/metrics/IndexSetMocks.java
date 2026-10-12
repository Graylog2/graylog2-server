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
package org.graylog2.indexer.indexset.metrics;

import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class IndexSetMocks {
    private IndexSetMocks() {
    }

    static IndexSet indexSet(String id, String prefix, boolean writable) {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(id);
        when(config.isWritable()).thenReturn(writable);
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getConfig()).thenReturn(config);
        when(indexSet.getIndexWildcard()).thenReturn(prefix + "_*");
        when(indexSet.getWriteIndexAlias()).thenReturn(prefix + "_deflector");
        return indexSet;
    }

    /**
     * Makes the service and registry resolve the IDs of the given index sets to them.
     */
    static void stubLookup(IndexSetService indexSetService, IndexSetRegistry indexSetRegistry, IndexSet... indexSets) {
        final Set<String> ids = Arrays.stream(indexSets).map(indexSet -> indexSet.getConfig().id()).collect(Collectors.toSet());
        final List<IndexSetConfig> configs = Arrays.stream(indexSets).map(IndexSet::getConfig).toList();
        when(indexSetService.findByIds(ids)).thenReturn(configs);
        when(indexSetRegistry.getFromIndexConfig(configs)).thenReturn(Set.of(indexSets));
    }
}
