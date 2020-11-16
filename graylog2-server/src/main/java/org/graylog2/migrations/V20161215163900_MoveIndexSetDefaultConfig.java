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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Updates.unset;

public class V20161215163900_MoveIndexSetDefaultConfig extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161215163900_MoveIndexSetDefaultConfig.class);

    private static final String FIELD_DEFAULT = "default";
    private static final String FIELD_CREATION_DATE = "creation_date";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ID = "_id";

    private final MongoCollection<Document> collection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20161215163900_MoveIndexSetDefaultConfig(final MongoConnection mongoConnection,
                                                     final ClusterConfigService clusterConfigService) {
        this.collection = mongoConnection.getMongoDatabase().getCollection("index_sets");
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-12-15T16:39:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already done.");
            return;
        }

        // Do not overwrite an existing default index config
        boolean defaultDone = clusterConfigService.get(DefaultIndexSetConfig.class) != null;
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

        final FindIterable<Document> documents = collection.find(exists(FIELD_DEFAULT)).sort(ascending(FIELD_CREATION_DATE));

        for (final Document document : documents) {
            final ObjectId id = document.getObjectId(FIELD_ID);
            final String idString = id.toHexString();
            final boolean isDefault = firstNonNull(document.getBoolean(FIELD_DEFAULT), false);

            if (!defaultDone && isDefault) {
                defaultDone = true;
                clusterConfigService.write(DefaultIndexSetConfig.create(idString));
            }

            final long modifiedCount = collection.updateOne(eq(FIELD_ID, id), unset(FIELD_DEFAULT)).getMatchedCount();

            if (modifiedCount > 0) {
                LOG.info("Removed <default> field from index set <{}> ({})", document.getString(FIELD_TITLE), idString);
                builder.add(idString);
            } else {
                LOG.error("Couldn't remove <default> field from index set <{}> ({})", document.getString(FIELD_TITLE), idString);
            }
        }

        clusterConfigService.write(MigrationCompleted.create(builder.build()));
    }

    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonProperty("index_set_ids")
        public abstract Set<String> indexSetIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("index_set_ids") Set<String> indexSetIds) {
            return new AutoValue_V20161215163900_MoveIndexSetDefaultConfig_MigrationCompleted (indexSetIds);
        }
    }
}
