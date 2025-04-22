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

import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.streams.StreamDTO;
import org.graylog2.streams.StreamServiceImpl;
import org.graylog2.streams.SystemStreamScope;

import java.time.ZonedDateTime;
import java.util.List;

import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;

public class V20250421184900_MigrateSystemStreams extends Migration {
       private final List<String> SYSTEM_STREAM_IDS = List.of(
            DEFAULT_EVENTS_STREAM_ID,
            DEFAULT_SYSTEM_EVENTS_STREAM_ID,
            FAILURES_STREAM_ID
    );

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<StreamDTO> collection;

    @Inject
    public V20250421184900_MigrateSystemStreams(ClusterConfigService clusterConfigService,
                                                MongoCollections mongoCollections) {
        this.clusterConfigService = clusterConfigService;
        this.collection = mongoCollections.collection(StreamServiceImpl.COLLECTION_NAME, StreamDTO.class);
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

        final Bson filter = MongoUtils.stringIdsIn(SYSTEM_STREAM_IDS);
        final Bson update = Updates.set(FIELD_SCOPE, SystemStreamScope.NAME);
        collection.updateMany(filter, update);

        clusterConfigService.write(new MigrationCompleted());
    }

    record MigrationCompleted() {}
}
