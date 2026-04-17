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
package org.graylog.integrations.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

public class V20260313163300_MigrateTeamsNotificationV2 extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260313163300_MigrateTeamsNotificationV2.class);
    private static final String COLLECTION_NAME = "event_notifications";
    private static final String TEAMS_V2 = "teams-notification-v2";
    private static final String TYPE_FIELD = "config.type";
    private static final String ADAPTIVE_CARD_FIELD = "config.adaptive_card";
    private static final String OLD_TIMESTAMP = "\"value\": \"${event.timestamp_processing}\"";
    private static final String NEW_TIMESTAMP = "\"value\": \"{{DATE(${event.timestamp_processing},SHORT)}} at {{TIME(${event.timestamp_processing})}}\"";

    private final MongoConnection mongoConnection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20260313163300_MigrateTeamsNotificationV2(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.mongoConnection = mongoConnection;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-03-13T16:33:00Z");
    }

    /**
     * This migration updates existing teams-notification-v2 configurations
     * Updates the adaptive card timestamp format to use DATE()/TIME() functions
     * for proper local time rendering in Teams.
     */
    @Override
    public void upgrade() {
        V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion completion = clusterConfigService.get(V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion.class);
        if (completion != null) {
            LOG.debug("Migration was already completed");
            return;
        }

        final MongoCollection<Document> collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        LOG.info("Updating '{}' collection.", COLLECTION_NAME);

        Bson v2Filter = Filters.eq(TYPE_FIELD, TEAMS_V2);

        // Update adaptive_card timestamp format for each matching doc
        collection.find(v2Filter).forEach(doc -> {
            Document config = doc.get("config", Document.class);
            if (config == null) return;

            String card = config.getString("adaptive_card");
            if (card == null || !card.contains(OLD_TIMESTAMP)) {
                LOG.debug("Skipping doc {}: adaptive_card already migrated or missing", doc.getObjectId("_id"));
                return;
            }

            String updatedCard = card.replace(OLD_TIMESTAMP, NEW_TIMESTAMP);
            Bson idFilter = Filters.eq("_id", doc.getObjectId("_id"));
            UpdateResult cardResult = collection.updateOne(
                    idFilter,
                    Updates.set(ADAPTIVE_CARD_FIELD, updatedCard)
            );
            LOG.info("Updated adaptive_card for doc {}: {}", doc.getObjectId("_id"), cardResult);
        });

        clusterConfigService.write(MigrationCompletion.create());
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class MigrationCompletion {
        @JsonCreator
        public static V20260313163300_MigrateTeamsNotificationV2.MigrationCompletion create() {
            return new AutoValue_V20260313163300_MigrateTeamsNotificationV2_MigrationCompletion();
        }
    }
}
