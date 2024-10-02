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
package org.graylog2.migrations;

import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.datanode.CurrentWriteIndices;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.indices.Indices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class V20240927120300_DataNodeMigrationIndexSetTest {

    @Mock
    MigrationStateMachine migrationStateMachine;
    @Mock
    IndexSetRegistry indexSetRegistry;
    @Mock
    Indices indices;

    V20240927120300_DataNodeMigrationIndexSet migration;

    @BeforeEach
    public void setup() {
        migration = new V20240927120300_DataNodeMigrationIndexSet(true,
                migrationStateMachine, indexSetRegistry, indices);
    }

    @Test
    public void testSkipWithoutDatanode() {
        migration = new V20240927120300_DataNodeMigrationIndexSet(false,
                migrationStateMachine, indexSetRegistry, indices);
        migration.upgrade();
        verifyNoInteractions(migrationStateMachine, indexSetRegistry, indices);
    }

    @Test
    public void testSkipWithoutExtendedStateProp() {
        MigrationStateMachineContext c = new MigrationStateMachineContext();
        when(migrationStateMachine.getContext()).thenReturn(c);
        migration.upgrade();
        verifyNoMoreInteractions(migrationStateMachine, indexSetRegistry, indices);
    }

    @Test
    public void testSkipIndexCreationWithIndexPresent() {
        MigrationStateMachineContext c = new MigrationStateMachineContext();
        CurrentWriteIndices writeIndices = new CurrentWriteIndices(Map.of("id1", "graylog_11"));
        c.addExtendedState(RemoteReindexingMigrationAdapter.EXISTING_INDEX_SET_WRITE_INDICES, writeIndices);
        when(migrationStateMachine.getContext()).thenReturn(c);
        IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getNewestIndex()).thenReturn("graylog_25");
        when(indexSetRegistry.get("id1")).thenReturn(Optional.of(indexSet));
        migration.upgrade();
        verifyNoMoreInteractions(indexSet);
        verifyNoInteractions(indices);
    }

    @Test
    public void testIndexCreatedWhenNotPresent() {
        MigrationStateMachineContext c = new MigrationStateMachineContext();
        CurrentWriteIndices writeIndices = new CurrentWriteIndices(Map.of("id1", "graylog_11"));
        c.addExtendedState(RemoteReindexingMigrationAdapter.EXISTING_INDEX_SET_WRITE_INDICES, writeIndices);
        when(migrationStateMachine.getContext()).thenReturn(c);
        IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getNewestIndex()).thenThrow(new NoTargetIndexException(""));
        when(indexSetRegistry.get("id1")).thenReturn(Optional.of(indexSet));
        migration.upgrade();
        verify(indices, times(1)).create("graylog_11", indexSet);
        verify(indexSet, times(1)).setUp();
    }

}
