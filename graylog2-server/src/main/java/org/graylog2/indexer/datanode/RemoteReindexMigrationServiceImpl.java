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

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.indexer.migration.LogEntry;
import org.mongojack.WriteResult;

import java.util.Locale;
import java.util.Optional;

public class RemoteReindexMigrationServiceImpl extends PaginatedDbService<MigrationConfiguration> implements RemoteReindexMigrationService {

    public static final String COLLECTION_NAME = "remote_reindex_migrations";

    @Inject
    public RemoteReindexMigrationServiceImpl(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, MigrationConfiguration.class, COLLECTION_NAME);
    }

    @Override
    public Optional<MigrationConfiguration> getMigration(String migrationId) {
        return get(migrationId);
    }

    @Override
    public MigrationConfiguration saveMigration(MigrationConfiguration migrationConfiguration) {
        return save(migrationConfiguration);
    }

    @Override
    public void assignTask(String migrationID, String indexName, String taskId) {
        final Bson filter = Filters.and(
                Filters.eq("_id", new ObjectId(migrationID)),
                Filters.eq("indices.indexName", indexName)
        );
        final Bson update = Updates.set("indices.$.taskId", taskId);
        if (!db.update(filter, update).isUpdateOfExisting()) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Failed to update migration %s. Index %s doesn't exist in the migration", migrationID, indexName));
        }
    }

    @Override
    public void appendLogEntry(String migrationId, LogEntry log) {
        final Bson filter = Filters.eq("_id", new ObjectId(migrationId));
        final Bson update = Updates.push("logs", log);
        final WriteResult<MigrationConfiguration, ObjectId> res = db.update(filter, update);
    }
}
