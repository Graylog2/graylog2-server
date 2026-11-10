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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

public class V20260223000000_RemovePerspectiveFromUserPreferences extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260223000000_RemovePerspectiveFromUserPreferences.class);
    private static final String PERSPECTIVE_KEY = "preferences.perspective";

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> usersCollection;

    @Inject
    public V20260223000000_RemovePerspectiveFromUserPreferences(ClusterConfigService clusterConfigService,
                                                                MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.usersCollection = mongoConnection.getMongoDatabase().getCollection("users");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-02-23T00:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final var result = usersCollection.updateMany(
                Filters.exists(PERSPECTIVE_KEY),
                Updates.unset(PERSPECTIVE_KEY)
        );

        LOG.info("Removed perspective attribute from {} user document(s).", result.getModifiedCount());
        clusterConfigService.write(new MigrationCompleted(result.getModifiedCount()));
    }

    public record MigrationCompleted(long modified) {}
}
