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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import one.util.streamex.EntryStream;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews extends Migration {
    private static final int DEFAULT_LIMIT = 15;
    private static final Logger LOG = LoggerFactory.getLogger(V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews.class);
    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> views;
    private final Document matchValuePivots = doc("config.type", "values");

    @Inject
    public V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.views = mongoConnection.getMongoDatabase().getCollection("views");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-01-13T09:53:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }

        final List<ViewWidgetLimitMigration> widgetLimitMigrations = StreamSupport.stream(this.views.find().spliterator(), false)
                .flatMap(document -> {
                    final String viewId = document.get("_id", ObjectId.class).toHexString();
                    final Map<String, Document> state = document.get("state", Collections.emptyMap());
                    return state.entrySet().stream()
                            .flatMap(entry -> {
                                final String queryId = entry.getKey();
                                final List<Document> widgets = entry.getValue().get("widgets", Collections.emptyList());
                                return EntryStream.of(widgets)
                                        .filter(widget -> "aggregation".equals(widget.getValue().getString("type")))
                                        .flatMap(widgetEntry -> {
                                            final Document widget = widgetEntry.getValue();
                                            final Integer widgetIndex = widgetEntry.getKey();
                                            final Document config = widget.get("config", new Document());
                                            final boolean hasRowLimit = config.containsKey("row_limit");
                                            final boolean hasColumnLimit = config.containsKey("column_limit");
                                            final Optional<Integer> rowLimit = Optional.ofNullable(config.getInteger("row_limit"));
                                            final Optional<Integer> columnLimit = Optional.ofNullable(config.getInteger("column_limit"));

                                            if (widgetIndex != null && (hasRowLimit || hasColumnLimit)) {
                                                return Stream.of(new ViewWidgetLimitMigration(viewId, queryId, widgetIndex, rowLimit, columnLimit));
                                            }
                                            return Stream.empty();
                                        });
                            });
                })
                .collect(Collectors.toList());

        final List<WriteModel<Document>> operations = widgetLimitMigrations.stream()
                .flatMap(widgetMigration -> {
                    final ImmutableList.Builder<WriteModel<Document>> builder = ImmutableList.builder();
                    builder.add(
                            updateView(
                                    widgetMigration.viewId,
                                    doc("$unset", doc(widgetConfigPath(widgetMigration) + ".row_limit", 1))
                            )
                    );
                    builder.add(
                            updateView(
                                    widgetMigration.viewId,
                                    doc("$set", doc(widgetConfigPath(widgetMigration) + ".row_pivots.$[config].config.limit", widgetMigration.rowLimit.orElse(DEFAULT_LIMIT))),
                                    matchValuePivots
                            )
                    );
                    builder.add(
                            updateView(
                                    widgetMigration.viewId,
                                    doc("$unset", doc(widgetConfigPath(widgetMigration) + ".column_limit", 1))
                            )
                    );
                    builder.add(
                            updateView(
                                    widgetMigration.viewId,
                                    doc("$set", doc(widgetConfigPath(widgetMigration) + ".column_pivots.$[config].config.limit", widgetMigration.columnLimit.orElse(DEFAULT_LIMIT))),
                                    matchValuePivots
                            )
                    );
                    return builder.build().stream();
                })
                .collect(Collectors.toList());

        if (!operations.isEmpty()) {
            LOG.debug("Updating {} widgets ...", widgetLimitMigrations.size());
            this.views.bulkWrite(operations);
        }

        clusterConfigService.write(new MigrationCompleted(widgetLimitMigrations.size()));
    }

    private String widgetConfigPath(ViewWidgetLimitMigration widgetMigration) {
        return "state." + widgetMigration.queryId() + ".widgets." + widgetMigration.widgetIndex() + ".config";
    }

    private WriteModel<Document> updateView(String viewId, Document update, List<Bson> arrayFilters) {
        return new UpdateOneModel<>(
                Filters.eq("_id", new ObjectId(viewId)),
                update,
                new UpdateOptions().upsert(false).arrayFilters(arrayFilters)
        );
    }

    private WriteModel<Document> updateView(String viewId, Document update, Bson arrayFilters) {
        return updateView(viewId, update, Collections.singletonList(arrayFilters));
    }

    private WriteModel<Document> updateView(String viewId, Document update) {
        return updateView(viewId, update, (List<Bson>) null);
    }

    private Document doc(String key, Object value) {
        return new Document(key, value);
    }

    public record ViewWidgetLimitMigration(String viewId, String queryId, Integer widgetIndex,
                                           Optional<Integer> rowLimit, Optional<Integer> columnLimit) {}

    public record MigrationCompleted(@JsonProperty("migrated_widgets") Integer migratedViews) {}
}
