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

import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.migration.IndexerConnectionCheckResult;
import org.graylog2.indexer.migration.RemoteReindexMigration;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

public class RemoteReindexingMigrationAdapterES7 implements RemoteReindexingMigrationAdapter {
    @Override
    public RemoteReindexMigration start(URI uri, String username, String password, List<String> indices, boolean synchronous) {
        throw new UnsupportedOperationException("Remote reindexing migrations are not supported for elasticsearch");
    }

    @Override
    public RemoteReindexMigration status(String migrationID) {
        throw new UnsupportedOperationException("Remote reindexing migrations are not supported for elasticsearch");
    }

    @Override
    public IndexerConnectionCheckResult checkConnection(URI uri, String username, String password) {
        throw new UnsupportedOperationException("Remote reindexing migrations are not supported for elasticsearch");
    }
}
