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

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SetIndexReadOnlyJobTest {

    private SetIndexReadOnlyJob toTest;

    @Mock
    private Indices indices;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    @Mock
    private OptimizeIndexJob.Factory optimizeIndexJobFactory;
    @Mock
    private ActivityWriter activityWriter;
    @Mock
    private IndexSet indexSet;
    @Mock
    private IndexSetConfig indexSetConfig;
    private final String index = "test_index";

    @BeforeEach
    void setUp() {
        toTest = new SetIndexReadOnlyJob(indices, indexSetRegistry, optimizeIndexJobFactory, activityWriter, index);
    }

    @Test
    void testDoesNothingIfIndexDoesNotExist() {
        doReturn(false).when(indices).exists(index);

        toTest.execute();

        verifyNoMoreInteractions(indices);
        verifyNoInteractions(indexSetRegistry);
        verifyNoInteractions(optimizeIndexJobFactory);
        verifyNoInteractions(activityWriter);
    }

    @Test
    void testDoesNothingIfIndexIsClosed() {
        doReturn(true).when(indices).exists(index);
        doReturn(true).when(indices).isClosed(index);

        toTest.execute();

        verifyNoMoreInteractions(indices);
        verifyNoInteractions(indexSetRegistry);
        verifyNoInteractions(optimizeIndexJobFactory);
        verifyNoInteractions(activityWriter);
    }

    @Test
    void testDoesNothingIfIndexIsNotPresentInIndexSetRegistry() {
        doReturn(true).when(indices).exists(index);
        doReturn(false).when(indices).isClosed(index);
        doReturn(Optional.empty()).when(indexSetRegistry).getForIndex(index);

        toTest.execute();

        verifyNoMoreInteractions(indices);
        verifyNoMoreInteractions(indexSetRegistry);
        verifyNoInteractions(optimizeIndexJobFactory);
        verifyNoInteractions(activityWriter);
    }

    @Test
    void testDoesNotOptimizeIndexOnOptimizationDisabled() {
        doReturn(true).when(indices).exists(index);
        doReturn(false).when(indices).isClosed(index);
        doReturn(indexSetConfig).when(indexSet).getConfig();
        doReturn(true).when(indexSetConfig).indexOptimizationDisabled();
        doReturn(Optional.of(indexSet)).when(indexSetRegistry).getForIndex(index);

        toTest.execute();
        verifyMainExecutionTasks();

        verifyNoInteractions(optimizeIndexJobFactory);
    }

    @Test
    void testCreatesAndExecutesIndexOptimizationJob() {
        doReturn(true).when(indices).exists(index);
        doReturn(false).when(indices).isClosed(index);
        doReturn(indexSetConfig).when(indexSet).getConfig();
        doReturn(13).when(indexSetConfig).indexOptimizationMaxNumSegments();
        doReturn(Optional.of(indexSet)).when(indexSetRegistry).getForIndex(index);
        final OptimizeIndexJob job = mock(OptimizeIndexJob.class);
        doReturn(job).when(optimizeIndexJobFactory).create(index, 13);

        toTest.execute();
        verifyMainExecutionTasks();

        verify(job).execute();

        verifyNoMoreInteractions(job);
        verifyNoMoreInteractions(optimizeIndexJobFactory);
    }

    private void verifyMainExecutionTasks() {
        verify(indices).flush(index);
        verify(indices).setReadOnly(index);
        verify(indices).setClosingDate(eq(index), any());
        verify(activityWriter).write(any());
    }
}
