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

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedForUserDTO;
import org.graylog.security.DBGrantService;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This is not really a migration but a deletion of existing elements in the grants collection.
 * LastOpened and Favorites were added as entities but shouldn't have been (because they don't get shared)
 */
public class V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection.class);
    private final MongoConnection mongoConnection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-31T13:55:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection.MigrationCompleted.class) != null) {
            return;
        }

        final Set<String> names = new HashSet();
        mongoConnection.getMongoDatabase().listCollectionNames().forEach(names::add);

        if (names.contains(DBGrantService.COLLECTION_NAME)) {
            var query = new BasicDBObject("target", Pattern.compile("^grn::::favorite:"));
            mongoConnection.getMongoDatabase().getCollection(DBGrantService.COLLECTION_NAME).deleteMany(query);
            query = new BasicDBObject("target", Pattern.compile("^grn::::last_opened:"));
            mongoConnection.getMongoDatabase().getCollection(DBGrantService.COLLECTION_NAME).deleteMany(query);
        }

        clusterConfigService.write(new MigrationCompleted());
    }


    public record MigrationCompleted() {}
}
