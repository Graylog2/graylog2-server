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
import com.mongodb.client.model.MergeOptions;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;

import static org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId.GENERAL_LAYOUT_VARIANT;

public class V20260428120000_AddLayoutVariantToEntityListPreferencesId extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260428120000_AddLayoutVariantToEntityListPreferencesId.class);
    static final String COLLECTION_NAME = "entity_list_preferences";

    private final ClusterConfigService clusterConfigService;
    private final MongoConnection mongoConnection;

    @Inject
    public V20260428120000_AddLayoutVariantToEntityListPreferencesId(final ClusterConfigService clusterConfigService,
                                                                     final MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-04-28T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final var collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        final var filter = Filters.exists("_id.layout_variant", false);
        final long toMigrate = collection.countDocuments(filter);

        if (toMigrate > 0) {
            collection.aggregate(List.of(
                    Aggregates.match(filter),
                    Aggregates.set(new Field<>("_id",
                            new Document("$mergeObjects", List.of(
                                    "$_id",
                                    new Document("layout_variant", GENERAL_LAYOUT_VARIANT)
                            ))
                    )),
                    Aggregates.merge(COLLECTION_NAME, new MergeOptions()
                            .whenMatched(MergeOptions.WhenMatched.KEEP_EXISTING)
                            .whenNotMatched(MergeOptions.WhenNotMatched.INSERT))
            )).toCollection();

            collection.deleteMany(filter);
        }

        clusterConfigService.write(new MigrationCompleted(toMigrate));
        LOG.info("Migrated {} entity_list_preferences documents to include layout_variant in _id.", toMigrate);
    }

    public record MigrationCompleted(long migratedDocuments) {}
}
