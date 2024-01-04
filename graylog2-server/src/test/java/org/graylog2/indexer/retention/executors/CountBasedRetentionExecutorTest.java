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

import org.graylog2.shared.system.activities.Activity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CountBasedRetentionExecutorTest extends AbstractRetentionExecutorTest {

    private CountBasedRetentionExecutor underTest;


    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        underTest = new CountBasedRetentionExecutor(indices, activityWriter, retentionExecutor);
    }

    @Test
    public void shouldRetainOldestIndex() {
        underTest.retain(indexSet, 5, action, "action");

        verify(action, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index1");

        verify(activityWriter, times(2)).write(any(Activity.class));
    }

    @Test
    public void shouldRetainOldestIndices() {
        underTest.retain(indexSet, 4, action, "action");

        verify(action, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        // Ensure that the oldest indices come first
        assertThat(retainedIndexName.getAllValues().get(0)).containsExactly("index1", "index2");

        verify(activityWriter, times(2)).write(any(Activity.class));
    }

    @Test
    public void shouldIgnoreReopenedIndexWhenCountingAgainstLimit() {
        when(indices.isReopened(eq("index1"))).thenReturn(true);

        underTest.retain(indexSet, 5, action, "action");

        verify(action, never()).retain(anyList(), eq(indexSet));

        verify(activityWriter, never()).write(any(Activity.class));
    }

    @Test
    public void shouldIgnoreWriteAliasWhenDeterminingRetainedIndices() {
        final String indexWithWriteIndexAlias = "index1";
        final String writeIndexAlias = "WriteIndexAlias";

        when(indexSet.getWriteIndexAlias()).thenReturn(writeIndexAlias);
        indexMap.put(indexWithWriteIndexAlias, Collections.singleton(writeIndexAlias));

        underTest.retain(indexSet, 5, action, "action");

        verify(action, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("index2");

        verify(activityWriter, times(2)).write(any(Activity.class));
    }


}
