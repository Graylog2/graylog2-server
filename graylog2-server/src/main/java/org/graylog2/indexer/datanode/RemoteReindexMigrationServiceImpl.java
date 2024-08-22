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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.indexer.migration.LogEntry;

import java.util.Locale;
import java.util.Optional;

import static org.graylog2.indexer.datanode.IndexMigrationConfiguration.FIELD_INDEX_NAME;
import static org.graylog2.indexer.datanode.IndexMigrationConfiguration.FIELD_TASK_ID;
import static org.graylog2.indexer.datanode.MigrationConfiguration.FIELD_INDICES;
import static org.graylog2.indexer.datanode.MigrationConfiguration.FIELD_LOGS;

public class RemoteReindexMigrationServiceImpl implements RemoteReindexMigrationService {

    public static final String COLLECTION_NAME = "remote_reindex_migrations";

    private final MongoCollection<MigrationConfiguration> collection;
    private final MongoUtils<MigrationConfiguration> mongoUtils;

    @Inject
    public RemoteReindexMigrationServiceImpl(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, MigrationConfiguration.class);
        this.mongoUtils = mongoCollections.utils(collection);
    }

    @Override
    public Optional<MigrationConfiguration> getMigration(String migrationId) {
        return mongoUtils.getById(migrationId);
    }

    @Override
    public MigrationConfiguration saveMigration(MigrationConfiguration migrationConfiguration) {
        var id = MongoUtils.insertedIdAsString(collection.insertOne(migrationConfiguration));
        return mongoUtils.getById(id).orElseThrow();
    }

    @Override
    public void assignTask(String migrationID, String indexName, String taskId) {
        final Bson filter = Filters.and(
                Filters.eq("_id", new ObjectId(migrationID)),
                Filters.eq(FIELD_INDICES + "." + FIELD_INDEX_NAME, indexName)
        );
        final Bson update = Updates.set(FIELD_INDICES + ".$." + FIELD_TASK_ID, taskId);
        if (collection.updateOne(filter, update).getModifiedCount() != 1) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Failed to update migration %s. Index %s doesn't exist in the migration", migrationID, indexName));
        }
    }

    @Override
    public void appendLogEntry(String migrationId, LogEntry log) {
        final Bson filter = Filters.eq("_id", new ObjectId(migrationId));
        final Bson update = Updates.push(FIELD_LOGS, log);
        if (collection.updateOne(filter, update).getModifiedCount() != 1) {
            throw new IllegalStateException("Failed to append log entry:" + log);
        }
    }

    @Override
    public Optional<String> getLatestMigrationId() {
        return Optional.ofNullable(collection.find().sort(new Document("_id", -1)).limit(1).first()).map(MigrationConfiguration::id);
    }
}
