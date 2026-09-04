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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.database.entities.NonDeletableSystemScope;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.time.ZonedDateTime;
import java.util.List;

import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;

public class V20250630180000_MigrateSystemStreams extends Migration {
    private final List<String> SYSTEM_STREAM_IDS = List.of(
            DEFAULT_EVENTS_STREAM_ID,
            DEFAULT_SYSTEM_EVENTS_STREAM_ID,
            FAILURES_STREAM_ID
    );

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> collection;

    @Inject
    public V20250630180000_MigrateSystemStreams(ClusterConfigService clusterConfigService,
                                                MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.collection = mongoConnection.getMongoDatabase().getCollection("streams", Document.class);
    }
    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-04-21T18:49:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            return;
        }

        collection.updateMany(MongoUtils.stringIdsIn(SYSTEM_STREAM_IDS), Updates.set(FIELD_SCOPE, ImmutableSystemScope.NAME));
        collection.updateOne(MongoUtils.idEq(DEFAULT_STREAM_ID), Updates.set(FIELD_SCOPE, NonDeletableSystemScope.NAME));


        clusterConfigService.write(new MigrationCompleted());
    }

    record MigrationCompleted() {}
}
