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
package org.graylog2.indexer.datanode;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.migration.IndexerConnectionCheckResult;
import org.graylog2.indexer.migration.RemoteReindexMigration;

import java.net.URI;
import java.util.Optional;

public interface RemoteReindexingMigrationAdapter {

    String EXISTING_INDEX_SET_WRITE_INDICES = "indexSetWriteIndices";
    boolean isMigrationRunning(IndexSet indexSet);

    enum Status {
        NOT_STARTED, RUNNING, ERROR, FINISHED
    }

    /**
     * @return ID of the migration, useful for obraining migration process info via {@link #status(String)}
     */
    String start(RemoteReindexRequest request);

    RemoteReindexMigration status(@Nonnull String migrationID);

    IndexerConnectionCheckResult checkConnection(@Nonnull final URI uri, @Nullable final String username, @Nullable final String password, @Nullable String allowlist, boolean trustUnknownCerts);

    Optional<String> getLatestMigrationId();
}
