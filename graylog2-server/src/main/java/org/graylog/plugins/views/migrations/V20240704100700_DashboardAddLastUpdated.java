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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class V20240704100700_DashboardAddLastUpdated extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190127111728_MigrateWidgetFormatSettings.class);
    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<ViewDTO> viewsCollection;
    private final MongoCollection<Search> searchesCollection;

    @Inject
    public V20240704100700_DashboardAddLastUpdated(final ClusterConfigService clusterConfigService, final MongoCollections mongoCollections) {
        this.clusterConfigService = clusterConfigService;
        this.viewsCollection = mongoCollections.collection("views", ViewDTO.class);
        this.searchesCollection = mongoCollections.collection("searches", Search.class);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-07-04T10:07:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final var views = viewsCollection.find(Filters.and(Filters.eq(ViewDTO.FIELD_LAST_UPDATED_AT, null), Filters.eq(ViewDTO.FIELD_TYPE, ViewDTO.Type.DASHBOARD))).into(new ArrayList<>());
        final var searchIds = views.stream().map(ViewDTO::searchId).toList();
        final var searches = searchesCollection.find(Filters.in("id", searchIds)).into(new ArrayList<>()).stream().collect(Collectors.toMap(Search::id, Function.identity()));;
        views.forEach(v -> {
                    final var search = searches.get(v.searchId());
                    final var n = v.toBuilder().lastUpdatedAt(search != null ? search.createdAt() : v.createdAt()).build();
                    viewsCollection.replaceOne(MongoUtils.idEq(n.id()), n);
                });

        clusterConfigService.write(new MigrationCompleted(DateTime.now(DateTimeZone.UTC)));
    }

    record MigrationCompleted(DateTime completedAt) {}
}
