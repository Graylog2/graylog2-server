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
package org.graylog.plugins.views.storage.migration.state.machine;

import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.datanode.CurrentWriteIndices;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MigrationShutdownService extends AbstractIdleService {

    private final Logger log = LoggerFactory.getLogger(AbstractIdleService.class);

    private final MigrationStateMachine migrationStateMachine;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public MigrationShutdownService(MigrationStateMachine migrationStateMachine,
                                    IndexSetRegistry indexSetRegistry) {
        this.migrationStateMachine = migrationStateMachine;
        this.indexSetRegistry = indexSetRegistry;
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
        if (List.of(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE,
                MigrationState.MIGRATE_EXISTING_DATA,
                MigrationState.REMOTE_REINDEX_RUNNING).contains(migrationStateMachine.getState())) {
            log.info("Storing active write indices for data node migration");
            Map<String, String> indices =
                    indexSetRegistry.getAll().stream()
                            .filter(indexSet -> indexSet.isUp() && indexSet.getConfig().isWritable() && Objects.nonNull(indexSet.getActiveWriteIndex()))
                            .collect(Collectors.toMap(is -> is.getConfig().id(), IndexSet::getActiveWriteIndex));
            migrationStateMachine.getContext().addExtendedState(RemoteReindexingMigrationAdapter.EXISTING_INDEX_SET_WRITE_INDICES,
                    new CurrentWriteIndices(indices));
            migrationStateMachine.saveContext();
        }
    }
}
