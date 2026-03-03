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
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Converts {@code fleet_id} fields in the {@code collector_instances} collection from String to ObjectId.
 * This can be removed once all test/dev deployments have been migrated.
 */
public class V20260303120000_ConvertCollectorInstanceFleetIdToObjectId extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260303120000_ConvertCollectorInstanceFleetIdToObjectId.class);
    private static final String COLLECTION_NAME = "collector_instances";
    private static final String FIELD_FLEET_ID = "fleet_id";

    private final MongoConnection mongoConnection;

    @Inject
    public V20260303120000_ConvertCollectorInstanceFleetIdToObjectId(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-03-03T12:00:00Z");
    }

    @Override
    public void upgrade() {
        final MongoCollection<Document> collection =
                mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);

        // Find documents where fleet_id is a string (not already an ObjectId)
        final var cursor = collection.find(Filters.type(FIELD_FLEET_ID, "string"));

        long converted = 0;
        for (Document doc : cursor) {
            final String fleetIdStr = doc.getString(FIELD_FLEET_ID);
            try {
                collection.updateOne(
                        Filters.eq("_id", doc.getObjectId("_id")),
                        Updates.set(FIELD_FLEET_ID, new ObjectId(fleetIdStr))
                );
                converted++;
            } catch (IllegalArgumentException e) {
                LOG.warn("Skipping collector instance {} — fleet_id '{}' is not a valid ObjectId",
                        doc.getObjectId("_id"), fleetIdStr);
            }
        }

        if (converted > 0) {
            LOG.info("Converted fleet_id from String to ObjectId in {} collector instance document(s)", converted);
        }
    }
}
