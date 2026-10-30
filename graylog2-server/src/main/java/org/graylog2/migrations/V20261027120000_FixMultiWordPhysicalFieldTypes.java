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
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Fixes physical field types persisted in the {@code index_field_types} collection with wrong names.
 * <p>
 * The OpenSearch 3 field type poller derived physical types from the enum constant names (e.g. {@code geopoint})
 * instead of their JSON values (e.g. {@code geo_point}), so these fields were reported as unknown type.
 * Fixed in PR #27959, but field types of existing non-write indices are never polled again, so we rename them here.
 */
public class V20261027120000_FixMultiWordPhysicalFieldTypes extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20261027120000_FixMultiWordPhysicalFieldTypes.class);

    private static final String COLLECTION_NAME = "index_field_types";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_PHYSICAL_TYPE = "physical_type";

    // Only the multi-word types known to the FieldTypeMapper, others are reported as unknown type either way
    static final Map<String, String> WRONG_TO_CORRECT_TYPES = Map.of(
            "geopoint", "geo_point",
            "halffloat", "half_float",
            "scaledfloat", "scaled_float"
    );

    private final MongoCollection<Document> collection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20261027120000_FixMultiWordPhysicalFieldTypes(MongoConnection mongoConnection,
                                                          ClusterConfigService clusterConfigService) {
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-10-27T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final List<Bson> updates = new ArrayList<>();
        final List<Bson> arrayFilters = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, String> type : WRONG_TO_CORRECT_TYPES.entrySet()) {
            final String identifier = f("t%d", i++);
            updates.add(Updates.set(f("%s.$[%s].%s", FIELD_FIELDS, identifier, FIELD_PHYSICAL_TYPE), type.getValue()));
            arrayFilters.add(Filters.eq(f("%s.%s", identifier, FIELD_PHYSICAL_TYPE), type.getKey()));
        }

        final UpdateResult result = collection.updateMany(
                Filters.in(f("%s.%s", FIELD_FIELDS, FIELD_PHYSICAL_TYPE), WRONG_TO_CORRECT_TYPES.keySet()),
                Updates.combine(updates),
                new UpdateOptions().arrayFilters(arrayFilters)
        );

        LOG.info("Fixed physical field types in {} index field type record(s).", result.getModifiedCount());
        clusterConfigService.write(new MigrationCompleted(result.getModifiedCount()));
    }

    public record MigrationCompleted(long modifiedRecords) {}
}
