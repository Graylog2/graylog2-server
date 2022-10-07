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
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import one.util.streamex.EntryStream;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class V20220929145442_MigratePivotLimitsInViews extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20220929145442_MigratePivotLimitsInViews.class);

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> views;

    @Inject
    public V20220929145442_MigratePivotLimitsInViews(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.views = mongoConnection.getMongoDatabase().getCollection("views");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-09-29T14:56:42Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }

        final List<ViewWidgetLimitMigration> widgetLimitMigrations = new ArrayList<>();
        for (Document document : this.views.find()) {
            final String viewId = document.get("_id", ObjectId.class).toHexString();
            final Map<String, Document> state = document.get("state", Collections.emptyMap());
            state.entrySet().stream()
                    .forEach(entry -> {
                        final String queryId = entry.getKey();
                        final List<Document> widgets = entry.getValue().get("widgets", Collections.emptyList());
                        EntryStream.of(widgets)
                                .filter(widget -> "aggregation".equals(widget.getValue().getString("type")))
                                .forEach(widgetEntry -> {
                                    final Document widget = widgetEntry.getValue();
                                    final Integer widgetIndex = widgetEntry.getKey();
                                    final Optional<Document> optionalConfig = Optional.ofNullable(widget.get("config", Document.class));
                                    optionalConfig.ifPresent(config -> {
                                        final List<Document> rowPivots = config.get("row_pivots", Collections.emptyList());
                                        final Optional<Integer> maxRowPivotLimit = extractMaxLimit(rowPivots);
                                        final List<Document> columnPivots = config.get("column_pivots", Collections.emptyList());
                                        final Optional<Integer> maxColumnPivotLimit = extractMaxLimit(columnPivots);

                                        if (widgetIndex != null && (maxRowPivotLimit.isPresent() || maxColumnPivotLimit.isPresent())) {
                                            widgetLimitMigrations.add(new ViewWidgetLimitMigration(viewId, queryId, widgetIndex, maxRowPivotLimit, maxColumnPivotLimit));
                                        }
                                    });
                                });
                    });
        }

        final List<WriteModel<Document>> operations = widgetLimitMigrations.stream()
                .flatMap(widgetMigration -> {
                    final ImmutableList.Builder<WriteModel<Document>> builder = ImmutableList.builder();
                    widgetMigration.rowLimit().ifPresent(rowLimit -> {
                        builder.add(updateView(widgetMigration.viewId,
                                new Document("$set", new Document(widgetConfigPath(widgetMigration) + ".row_limit", rowLimit))
                        ));
                        builder.add(updateView(widgetMigration.viewId,
                                new Document("$unset", new Document(widgetConfigPath(widgetMigration) + ".row_pivots.$[].config.limit", 1))
                        ));
                    });
                    widgetMigration.columnLimit().ifPresent(columnLimit -> {
                        builder.add(updateView(widgetMigration.viewId,
                                new Document("$set", new Document(widgetConfigPath(widgetMigration) + ".column_limit", columnLimit))
                        ));
                        builder.add(updateView(widgetMigration.viewId,
                                new Document("$unset", new Document(widgetConfigPath(widgetMigration) + ".column_pivots.$[].config.limit", 1))
                        ));
                    });
                    return builder.build().stream();
                })
                .collect(Collectors.toList());

        if (!operations.isEmpty()) {
            this.views.bulkWrite(operations);
        }

        clusterConfigService.write(new MigrationCompleted(widgetLimitMigrations.size()));
    }

    private String widgetConfigPath(ViewWidgetLimitMigration widgetMigration) {
        return "state." + widgetMigration.queryId() + ".widgets." + widgetMigration.widgetIndex() + ".config";
    }

    private WriteModel<Document> updateView(String viewId, Document update) {
        return new UpdateOneModel<>(
                new Document("_id", new ObjectId(viewId)),
                update,
                new UpdateOptions().upsert(false)
        );
    }

    private Optional<Integer> extractMaxLimit(List<Document> pivots) {
        return pivots.stream()
                .filter(pivot -> "values".equals(pivot.get("type")))
                .map(pivot -> pivot.get("config", Document.class))
                .map(pivotConfig -> pivotConfig != null ? pivotConfig.getInteger("limit") : null)
                .filter(Objects::nonNull)
                .max(Integer::compare);
    }

    public record ViewWidgetLimitMigration(String viewId, String queryId, Integer widgetIndex, Optional<Integer> rowLimit, Optional<Integer> columnLimit) {}

    public record MigrationCompleted(@JsonProperty("migrated_widgets") Integer migratedViews) {}
}
