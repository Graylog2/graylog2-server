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

import jakarta.inject.Inject;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.datanode.CurrentWriteIndices;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.indices.Indices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Migration creating the default index set from the legacy settings.
 */
public class V20240927120300_DataNodeMigrationIndexSet extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20240927120300_DataNodeMigrationIndexSet.class);

    private final Boolean runsWithDataNode;
    private final MigrationStateMachine migrationStateMachine;
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;

    @Inject
    public V20240927120300_DataNodeMigrationIndexSet(@RunsWithDataNode Boolean runsWithDataNode, MigrationStateMachine migrationStateMachine, IndexSetRegistry indexSetRegistry, Indices indices) {
        this.runsWithDataNode = runsWithDataNode;
        this.migrationStateMachine = migrationStateMachine;
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-09-27T12:03:00Z");
    }

    @Override
    public void upgrade() {
        if (!runsWithDataNode) {
            return;
        }
        migrationStateMachine.getContext()
                .getExtendedState(RemoteReindexingMigrationAdapter.EXISTING_INDEX_SET_WRITE_INDICES, CurrentWriteIndices.class)
                .ifPresent(currentWriteIndices -> {
                    currentWriteIndices.writeIndices().forEach((indexSetId, currentWriteIndex) -> {
                        indexSetRegistry.get(indexSetId).ifPresent(indexSet -> {
                            try {
                                indexSet.getNewestIndex();
                            } catch (NoTargetIndexException e) {
                                LOG.info("No existing index found for {}, creating last known index now", indexSetId);
                                indices.create(currentWriteIndex, indexSet);
                                indexSet.setUp();
                            }
                        });
                    });
                    migrationStateMachine.getContext().removeExtendedState(RemoteReindexingMigrationAdapter.EXISTING_INDEX_SET_WRITE_INDICES);
                    migrationStateMachine.saveContext();
                });
    }

}
