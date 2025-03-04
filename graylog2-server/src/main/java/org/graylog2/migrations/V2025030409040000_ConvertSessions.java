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
import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.sessions.MongoDbSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Migrates session attributes from byte[] to Map<String,Object>.
 */
public class V2025030409040000_ConvertSessions extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V2025030409040000_ConvertSessions.class);

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> collection;

    @Inject
    public V2025030409040000_ConvertSessions(ClusterConfigService clusterConfigService,
                                             MongoCollections mongoCollections) {
        this.clusterConfigService = clusterConfigService;
        this.collection = mongoCollections.nonEntityCollection(MongoDbSessionService.COLLECTION_NAME, Document.class);
    }

    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-03-04T09:04:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }

        MongoUtils.stream(collection.find(Filters.type("attributes", BsonType.BINARY)))
                .forEach(doc -> collection.updateOne(
                        Filters.eq("_id", doc.get("_id", ObjectId.class)),
                        Updates.set("attributes", toMap(doc.get("attributes", byte[].class)))));

        clusterConfigService.write(new MigrationCompleted());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(byte[] attributes) {
        try (final var bais = new ByteArrayInputStream(attributes); final var ois = new ObjectInputStream(bais)) {
            final Map<Object, Object> attributesMap = (Map<Object, Object>) ois.readObject();
            return attributesMap.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        } catch (Exception e) {
            LOG.error("Failed to deserialize attributes", e);
            return Map.of();
        }
    }

    public record MigrationCompleted() {}
}
