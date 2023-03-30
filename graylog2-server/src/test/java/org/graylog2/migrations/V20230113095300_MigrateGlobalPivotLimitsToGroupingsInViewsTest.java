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
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViewsTest {
    private final V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews migration;
    private final MongoCollection<Document> collection;

    private final ClusterConfigService clusterConfigService;

    public V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViewsTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        this.collection = mongoConnection.getMongoDatabase().getCollection("views");
        this.migration = new V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews(mongoConnection, clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }

    @Test
    @MongoDBFixtures("V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViewsTest_empty.json")
    void notMigratingAnythingIfViewsAreEmpty() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedViews()).isZero();
    }

    @Test
    @MongoDBFixtures("V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViewsTest_multiplePivots.json")
    void migratingMultiplePivots() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedViews()).isEqualTo(4);

        final Document document = this.collection.find().first();

        final List<Document> widgets = getWidgets(document);

        assertThat(rowPivotLimits(widgets.get(0))).containsExactly(3);
        assertThat(columnPivotLimits(widgets.get(0))).containsExactly(10);

        assertThat(rowPivotLimits(widgets.get(1))).containsExactly(20, null, 20);
        assertThat(columnPivotLimits(widgets.get(1))).isEmpty();

        assertThat(rowPivotLimits(widgets.get(2))).containsExactly(15, 15, 15);
        assertThat(columnPivotLimits(widgets.get(2))).isEmpty();

        assertThat(rowPivotLimits(widgets.get(3))).isEmpty();
        assertThat(columnPivotLimits(widgets.get(3))).containsExactly(null, 15, 15);

        assertThat(migrationCompleted().migratedViews()).isEqualTo(4);

        for (Document widget : widgets) {
            assertThatFieldsAreUnset(widget);
        }
    }

    @Test
    @MongoDBFixtures("V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViewsTest_null_limits.json")
    void migratingPivotsWithNullLimits() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedViews()).isEqualTo(1);

        final Document document = this.collection.find().first();

        final List<Document> widgets = getWidgets(document);

        for (Document widget : widgets) {
            assertThatFieldsAreUnset(widget);
        }
    }

    private void assertThatFieldsAreUnset(Document widget) {
        assertThat(widget.getEmbedded(List.of("config", "row_limit"), Integer.class)).isNull();
        assertThat(widget.getEmbedded(List.of("config", "column_limit"), Integer.class)).isNull();
    }

    private List<Integer> columnPivotLimits(Document widget) {
        final List<Document> columnPivots = widget.getEmbedded(List.of("config", "column_pivots"), Collections.emptyList());
        return columnPivots.stream()
                .map(rowPivot -> rowPivot.getEmbedded(List.of("config", "limit"), Integer.class))
                .collect(Collectors.toList());
    }

    private List<Integer> rowPivotLimits(Document widget) {
        final List<Document> rowPivots = widget.getEmbedded(List.of("config", "row_pivots"), Collections.emptyList());
        return rowPivots.stream()
                .map(rowPivot -> rowPivot.getEmbedded(List.of("config", "limit"), Integer.class))
                .collect(Collectors.toList());
    }

    private V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews.MigrationCompleted migrationCompleted() {
        final ArgumentCaptor<V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews.MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews.MigrationCompleted.class);
        verify(this.clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private List<Document> getWidgets(@Nullable Document document) {
        return document.getEmbedded(List.of("state", "e4a3962e-7477-4ea0-8290-48a0da8b2c78", "widgets"), List.class);
    }

    private List<Document> rowPivots(@Nullable Document widget) {
        return widget.getEmbedded(List.of("config", "row_pivots"), List.class);
    }

    private List<Document> columnPivots(@Nullable Document widget) {
        return widget.getEmbedded(List.of("config", "column_pivots"), List.class);
    }
}
