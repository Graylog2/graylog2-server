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
package org.graylog.storage.elasticsearch7;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.datanode.RemoteReindexRequest;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.migration.IndexerConnectionCheckResult;
import org.graylog2.indexer.migration.RemoteReindexMigration;

import java.net.URI;
import java.util.Optional;

public class UnsupportedRemoteReindexingMigrationAdapterES7 implements RemoteReindexingMigrationAdapter {

    public static final String UNSUPPORTED_MESSAGE = "This operation should never be called. We remote-reindex into the DataNode that contains OpenSearch. This adapter only exists for API completeness";

    @Override
    public boolean isMigrationRunning(IndexSet indexSet) {
        return false; // we'll never run a remote reindex migration against elasticsearch target. It's always OS in datanode.
    }

    @Override
    public String start(RemoteReindexRequest request) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public RemoteReindexMigration status(@Nonnull String migrationID) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public IndexerConnectionCheckResult checkConnection(@Nonnull URI uri, @Nullable String username, @Nullable String password, @Nullable String allowlist, boolean trustUnknownCerts) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Optional<String> getLatestMigrationId() {
        return Optional.empty();
    }
}
