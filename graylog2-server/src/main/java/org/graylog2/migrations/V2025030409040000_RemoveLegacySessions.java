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

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import jakarta.inject.Inject;
import org.bson.BsonType;
import org.bson.Document;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.security.SessionDeletedEvent;
import org.graylog2.security.sessions.MongoDbSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes sessions which use the legacy serialization format.
 * This migration intentionally runs on every start to delete legacy sessions which might have been persisted due
 * to a rolling version upgrade. Rolling version upgrades are not supported but if performed nevertheless, legacy
 * sessions would be hard to get rid of.
 */
public class V2025030409040000_RemoveLegacySessions extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V2025030409040000_RemoveLegacySessions.class);

    private final MongoCollection<Document> collection;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public V2025030409040000_RemoveLegacySessions(MongoCollections mongoCollections, ClusterEventBus clusterEventBus) {
        this.collection = mongoCollections.nonEntityCollection(MongoDbSessionService.COLLECTION_NAME, Document.class);
        this.clusterEventBus = clusterEventBus;
    }

    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-03-04T09:04:00Z");
    }

    @Override
    public void upgrade() {
        final var stopwatch = Stopwatch.createStarted();
        final List<String> sessionIds = collection.find(Filters.type("attributes", BsonType.BINARY))
                .projection(Projections.include("session_id"))
                .map(document -> document.getString("session_id"))
                .into(new ArrayList<>());

        LOG.debug("Found {} legacy sessions.", sessionIds.size());

        Lists.partition(sessionIds, 100).forEach(partition -> {
            LOG.debug("Removing {} legacy sessions from MongoDB.", partition.size());
            collection.deleteMany(Filters.in("session_id", sessionIds));
        });

        LOG.debug("Posting {} SessionDeletedEvents to the cluster event bus.", sessionIds.size());
        sessionIds.forEach(sessionId -> clusterEventBus.post(new SessionDeletedEvent(sessionId)));

        if (!sessionIds.isEmpty()) {
            LOG.info("Removed {} legacy sessions in {}ms", sessionIds.size(), stopwatch.elapsed().toMillis());
        }
    }
}
