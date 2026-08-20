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
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Removes stray {@code fields} sub-documents from the {@code inputs} collection.
 * <p>
 * A missing {@code @JsonIgnore} on {@code Input.getFields()} (introduced in the
 * {@code InputImpl} AutoValue migration, PR #24057) caused Jackson/mongojack to
 * persist a recursive {@code fields} key on every input save during 7.1.0 pre-release
 * builds. PR #25673 fixed the serialization; this migration cleans up the leftover data.
 */
public class V20260416120000_RemoveStrayFieldsFromInputs extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260416120000_RemoveStrayFieldsFromInputs.class);

    private static final String FIELD_TO_REMOVE = "fields";

    private final MongoCollection<Document> collection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20260416120000_RemoveStrayFieldsFromInputs(MongoConnection mongoConnection,
                                                       ClusterConfigService clusterConfigService) {
        this.collection = mongoConnection.getMongoDatabase().getCollection("inputs");
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-04-16T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final UpdateResult result = collection.updateMany(
                Filters.exists(FIELD_TO_REMOVE),
                Updates.unset(FIELD_TO_REMOVE)
        );

        LOG.info("Removed stray <fields> sub-document from {} input(s).", result.getModifiedCount());

        clusterConfigService.write(new MigrationCompleted(result.getModifiedCount()));
    }

    public record MigrationCompleted(long modifiedInputs) {}
}
