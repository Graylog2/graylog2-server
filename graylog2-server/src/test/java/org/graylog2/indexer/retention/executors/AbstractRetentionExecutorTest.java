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
package org.graylog2.indexer.retention.executors;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractRetentionExecutorTest {

    final DateTime NOW = DateTime.now(DateTimeZone.UTC);

    @Mock
    Indices indices;
    @Mock
    ActivityWriter activityWriter;
    @Mock
    IndexSet indexSet;
    @Mock
    RetentionExecutor.RetentionAction action;
    @Captor
    ArgumentCaptor<List<String>> retainedIndexName;

    Map<String, Set<String>> indexMap;
    RetentionExecutor retentionExecutor;

    public void setUp() throws Exception {
        indexMap = new HashMap<>();
        indexMap.put("index1", Collections.emptySet());
        indexMap.put("index2", Collections.emptySet());
        indexMap.put("index3", Collections.emptySet());
        indexMap.put("index4", Collections.emptySet());
        indexMap.put("index5", Collections.emptySet());
        indexMap.put("index6", Collections.emptySet());

        lenient().when(indices.indexClosingDate("index6")).thenReturn(Optional.of(NOW.minusDays(1)));
        lenient().when(indices.indexClosingDate("index5")).thenReturn(Optional.of(NOW.minusDays(3)));
        lenient().when(indices.indexClosingDate("index4")).thenReturn(Optional.of(NOW.minusDays(9)));
        lenient().when(indices.indexClosingDate("index3")).thenReturn(Optional.of(NOW.minusDays(10)));
        lenient().when(indices.indexClosingDate("index2")).thenReturn(Optional.of(NOW.minusDays(11)));
        lenient().when(indices.indexClosingDate("index1")).thenReturn(Optional.of(NOW.minusDays(15)));

        when(indexSet.getAllIndexAliases()).thenReturn(indexMap);
        lenient().when(indexSet.getManagedIndices()).thenReturn(indexMap.keySet().toArray(String[]::new));
        lenient().when(indexSet.extractIndexNumber(anyString())).then(this::extractIndexNumber);

        // Report all but the newest index as read-only
        lenient().when(indices.getIndicesBlocksStatus(anyList())).then(a -> {
            final List<String> indices = a.getArgument(0);
            final IndicesBlockStatus indicesBlockStatus = new IndicesBlockStatus();
            final String newestIndex = "index6";
            indices.forEach(i -> {
                if (!newestIndex.equals(i)) {
                    indicesBlockStatus.addIndexBlocks(i, Set.of("index.blocks.write"));
                }
            });
            return indicesBlockStatus;
        });
        retentionExecutor = new RetentionExecutor(activityWriter, indices);
    }

    private Optional<Integer> extractIndexNumber(InvocationOnMock invocation) {
        return Optional.of(Integer.parseInt(((String) invocation.getArgument(0)).replace("index", "")));
    }
}
