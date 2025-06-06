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
package org.graylog.plugins.views.migrations;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class V20240626143000_CreateDashboardsView extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190127111728_MigrateWidgetFormatSettings.class);
    private static final String DASHBOARDS_COLLECTION = "dashboards";
    private final ClusterConfigService clusterConfigService;
    private final MongoConnection mongoConnection;

    @Inject
    public V20240626143000_CreateDashboardsView(ClusterConfigService clusterConfigService, MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-06-26T12:30:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        final var database = mongoConnection.getMongoDatabase();

        // On setups which were running Graylog before Views were introduced, a `dashboards` collection might exist
        final var collectionExists = database.listCollectionNames().into(new ArrayList<>()).contains(DASHBOARDS_COLLECTION);

        if (collectionExists) {
            database.getCollection(DASHBOARDS_COLLECTION).drop();
        }

        database.createView(DASHBOARDS_COLLECTION, ViewDTO.COLLECTION_NAME, List.of(
                Aggregates.match(Filters.eq(ViewDTO.FIELD_TYPE, ViewDTO.Type.DASHBOARD))
        ));

        clusterConfigService.write(new MigrationCompleted(DateTime.now(DateTimeZone.UTC)));
    }

    record MigrationCompleted(DateTime completedAt) {}
}
