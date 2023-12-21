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

import org.graylog.plugins.views.favorites.FavoritesService;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedService;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * This is not really a migration but a deletion of existing contents by dropping the collections.
 * The background is: existing collections only exist on dev machines and intermediate systems but not on real
 * environments (the startpage has not been released yet) so dropping the collections is ok.
 */
public class V20230220095500_MigrateStartPageObjectReferencesToGRNbyRemoval extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230220095500_MigrateStartPageObjectReferencesToGRNbyRemoval.class);
    private final MongoConnection mongoConnection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20230220095500_MigrateStartPageObjectReferencesToGRNbyRemoval(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-02-20T09:55:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20230220095500_MigrateStartPageObjectReferencesToGRNbyRemoval.MigrationCompleted.class) != null) {
            return;
        }

        final Set<String> names = new HashSet();
        mongoConnection.getMongoDatabase().listCollectionNames().forEach(names::add);

        if (names.contains(FavoritesService.COLLECTION_NAME)) {
            mongoConnection.getMongoDatabase().getCollection(FavoritesService.COLLECTION_NAME).drop();
        }
        if (names.contains(LastOpenedService.COLLECTION_NAME)) {
            mongoConnection.getMongoDatabase().getCollection(LastOpenedService.COLLECTION_NAME).drop();
        }
        if (names.contains(RecentActivityService.COLLECTION_NAME)) {
            mongoConnection.getMongoDatabase().getCollection(RecentActivityService.COLLECTION_NAME).drop();
        }

        clusterConfigService.write(new MigrationCompleted());
    }


    public record MigrationCompleted() {}
}
