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
package org.graylog.collectors.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.collectors.CollectorsConfig;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_ENROLLMENT_TOKEN_ID;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_LAST_SEEN;

public class V20260316000000_MigrateCollectorsData extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260316000000_MigrateCollectorsData.class);
    private static final String CLUSTER_CONFIG_COLLECTION = "cluster_config";
    private static final String INSTANCES_COLLECTION = "collector_instances";
    private static final String CONFIG_TYPE = "org.graylog.collectors.CollectorsConfig";

    private final MongoConnection mongoConnection;

    @Inject
    public V20260316000000_MigrateCollectorsData(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-03-16T00:00:00Z");
    }

    @Override
    public void upgrade() {
        backfillThresholdDefaults();
        convertLastSeenToBsonDate();
        backfillEnrollmentTokenId();
    }

    private void backfillThresholdDefaults() {
        final var collection = mongoConnection.getMongoDatabase().getCollection(CLUSTER_CONFIG_COLLECTION);
        final Document doc = collection.find(Filters.eq("type", CONFIG_TYPE)).first();
        if (doc == null) {
            LOG.debug("No collectors config found, skipping threshold backfill.");
            return;
        }

        final Document payload = doc.get("payload", Document.class);
        if (payload == null) {
            return;
        }

        final List<Bson> updates = new ArrayList<>();

        if (!payload.containsKey("collector_offline_threshold")) {
            updates.add(Updates.set("payload.collector_offline_threshold",
                    CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD.toString()));
        }

        if (!payload.containsKey("collector_default_visibility_threshold")) {
            updates.add(Updates.set("payload.collector_default_visibility_threshold",
                    CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD.toString()));
        }

        if (!payload.containsKey("collector_expiration_threshold")) {
            updates.add(Updates.set("payload.collector_expiration_threshold",
                    CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD.toString()));
        }

        if (!updates.isEmpty()) {
            collection.updateOne(Filters.eq("type", CONFIG_TYPE), Updates.combine(updates));
            LOG.info("Backfilled collectors config threshold defaults.");
        }
    }

    private void backfillEnrollmentTokenId() {
        final MongoCollection<Document> collection =
                mongoConnection.getMongoDatabase().getCollection(INSTANCES_COLLECTION);

        final long updated = collection.updateMany(
                Filters.not(Filters.exists(FIELD_ENROLLMENT_TOKEN_ID)),
                Updates.set(FIELD_ENROLLMENT_TOKEN_ID, "unknown")
        ).getModifiedCount();

        if (updated > 0) {
            LOG.info("Backfilled enrollment_token_id in {} collector instance document(s).", updated);
        }
    }

    private void convertLastSeenToBsonDate() {
        final MongoCollection<Document> collection =
                mongoConnection.getMongoDatabase().getCollection(INSTANCES_COLLECTION);

        long converted = 0;

        for (final Document doc : collection.find(Filters.type(FIELD_LAST_SEEN, "string"))) {
            final String value = doc.getString(FIELD_LAST_SEEN);
            final Date date = Date.from(Instant.parse(value));
            collection.updateOne(
                    Filters.eq("_id", doc.getObjectId("_id")),
                    Updates.set(FIELD_LAST_SEEN, date)
            );
            converted++;
        }

        if (converted > 0) {
            LOG.info("Converted last_seen to BSON Date in {} collector instance document(s).", converted);
        }
    }
}
