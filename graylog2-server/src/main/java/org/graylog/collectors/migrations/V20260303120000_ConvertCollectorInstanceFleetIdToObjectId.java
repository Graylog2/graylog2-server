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
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamRuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Converts {@code fleet_id} and {@code issuing_ca_id} fields in the {@code collector_instances} collection
 * from ObjectId back to String for consistency with the rest of the codebase.
 * This can be removed once all deployments have been migrated.
 */
public class V20260303120000_ConvertCollectorInstanceFleetIdToObjectId extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260303120000_ConvertCollectorInstanceFleetIdToObjectId.class);
    private static final String COLLECTION_NAME = "collector_instances";
    private static final String FIELD_FLEET_ID = "fleet_id";
    private static final String FIELD_ISSUING_CA_ID = "issuing_ca_id";

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
        final var db = mongoConnection.getMongoDatabase();
        final MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);

        long converted = 0;
        converted += convertObjectIdFieldToString(collection, FIELD_FLEET_ID);
        converted += convertObjectIdFieldToString(collection, FIELD_ISSUING_CA_ID);

        if (converted > 0) {
            LOG.info("Converted ObjectId fields to String in {} collector instance document(s)", converted);
        }

        final var renamedCollections = Map.of(
                "fleets", "collector_fleets",
                "fleet_transaction_log", "collector_fleet_transaction_log",
                "fleet_sources", "collector_fleet_sources"
        );

        for (final var entry : renamedCollections.entrySet()) {
            final var fromColl = db.getCollection(entry.getKey());
            final var toColl = db.getCollection(entry.getValue());

            if (fromColl.countDocuments() > 0) {
                LOG.info("Migrating collection data from <{}> to <{}>", fromColl.getNamespace(), toColl.getNamespace());
                fromColl.find().forEach(doc -> {
                    LOG.info("  Document {}", doc.get("_id"));
                    toColl.insertOne(doc);
                    fromColl.deleteOne(Filters.eq("_id", doc.get("_id")));
                });

                if (fromColl.countDocuments() == 0) {
                    LOG.info("Dropping old collection <{}>", fromColl.getNamespace());
                    fromColl.drop();
                }
            }
        }

        renameCollectorsConfigFields(db);

        // We renamed the field that contains the source type
        final var updateResult = mongoConnection.getMongoDatabase().getCollection("streamrules")
                .updateOne(
                        Filters.eq(StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(Stream.COLLECTOR_LOGS_STREAM_ID)),
                        Updates.set(StreamRuleImpl.FIELD_FIELD, CollectorIngestCodec.FIELD_COLLECTOR_SOURCE_TYPE)
                );
        if (updateResult.getModifiedCount() > 0) {
            LOG.info("Updated Collector stream rule to match on field <{}>", CollectorIngestCodec.FIELD_COLLECTOR_SOURCE_TYPE);
        }
    }

    private static Optional<Document> loadCollectorsConfig(MongoDatabase db) {
        return Optional.ofNullable(db.getCollection("cluster_config")
                .find(Filters.eq("type", "org.graylog.collectors.CollectorsConfig"))
                .first());
    }

    private static void renameCollectorsConfigFields(MongoDatabase db) {
        final var oldName = "opamp_ca_id";
        final var newName = "signing_cert_id";
        final var payload = loadCollectorsConfig(db).map(c -> c.get("payload", Document.class));

        if (payload.isPresent() && isNotBlank(payload.get().getString(oldName))) {
            final var result = db.getCollection("cluster_config").updateOne(
                    Filters.eq("type", "org.graylog.collectors.CollectorsConfig"),
                    Updates.combine(
                            Updates.set("payload." + newName, payload.get().getString(oldName)),
                            Updates.unset("payload." + oldName)
                    )
            );
            if (result.getModifiedCount() > 0) {
                LOG.info("Renamed CollectorsConfig field <{}> to <{}>", oldName, newName);
            } else {
                LOG.warn("Couldn't rename CollectorsConfig field <{}> to <{}>", oldName, newName);
            }
        }
    }

    private long convertObjectIdFieldToString(MongoCollection<Document> collection, String fieldName) {
        final var cursor = collection.find(Filters.type(fieldName, "objectId"));

        long converted = 0;
        for (final Document doc : cursor) {
            final ObjectId objectId = doc.getObjectId(fieldName);
            collection.updateOne(
                    Filters.eq("_id", doc.getObjectId("_id")),
                    Updates.set(fieldName, objectId.toHexString())
            );
            converted++;
        }

        return converted;
    }
}
