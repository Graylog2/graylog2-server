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
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.List;

public class V20230904073300_MigrateThemePreferences extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230904073300_MigrateThemePreferences.class);
    private static final String THEME_MODE_KEY = "preferences.themeMode";
    private final ClusterConfigService configService;
    private final MongoCollection<Document> usersCollections;

    @Inject
    public V20230904073300_MigrateThemePreferences(ClusterConfigService configService,
                                                   MongoConnection mongoConnection) {
        this.configService = configService;
        this.usersCollections = mongoConnection.getMongoDatabase().getCollection("users");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-09-04T07:33:00Z");
    }

    @Override
    public void upgrade() {
        if (configService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        var result = this.usersCollections.bulkWrite(List.of(
                new UpdateManyModel<>(Filters.eq(THEME_MODE_KEY, "teint"), Updates.set(THEME_MODE_KEY, "light")),
                new UpdateManyModel<>(Filters.eq(THEME_MODE_KEY, "noir"), Updates.set(THEME_MODE_KEY, "dark"))
        ));

        if (result.wasAcknowledged()) {
            configService.write(new MigrationCompleted(result.getMatchedCount(), result.getModifiedCount()));
        }
    }

    public record MigrationCompleted(long matched, long modified) {}
}
