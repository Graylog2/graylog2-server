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

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.bson.BsonType;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Arrays;

public class V20251103123300_MigrateEntityListPreferences extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202406260800_MigrateCertificateAuthority.class);
    static final String COLLECTION_NAME = "entity_list_preferences";
    private static final String FIELD_DISPLAYED_ATTRIBUTES = "displayed_attributes";
    private static final String FIELD_ATTRIBUTES = "attributes";

    private final ClusterConfigService clusterConfigService;
    private final MongoConnection mongoConnection;

    @Inject
    public V20251103123300_MigrateEntityListPreferences(ClusterConfigService clusterConfigService, MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-11-03T12:33:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final var collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        final var filter = Filters.and(
                Filters.exists(FIELD_DISPLAYED_ATTRIBUTES, true),
                Filters.type(FIELD_DISPLAYED_ATTRIBUTES, BsonType.ARRAY)
        );

        final var updatePipeline = Arrays.asList(
                Aggregates.set(new Field<>(FIELD_ATTRIBUTES,
                        new Document("$cond", Arrays.asList(
                                new Document("$isArray", "$" + FIELD_DISPLAYED_ATTRIBUTES),
                                new Document("$arrayToObject",
                                        new Document("$map", new Document("input", "$" + FIELD_DISPLAYED_ATTRIBUTES)
                                                .append("as", "attr")
                                                .append("in", new Document("k", "$$attr")
                                                        .append("v", new Document("status", "show"))
                                                )
                                        )
                                ),
                                "$" + FIELD_ATTRIBUTES
                        ))
                )),
                Aggregates.unset(FIELD_DISPLAYED_ATTRIBUTES)
        );

        var result = collection.updateMany(filter, updatePipeline);
        if (result.wasAcknowledged()) {
            clusterConfigService.write(new MigrationCompleted(result.getModifiedCount()));
        }
    }

    public record MigrationCompleted(long updatedTables) {}
}
