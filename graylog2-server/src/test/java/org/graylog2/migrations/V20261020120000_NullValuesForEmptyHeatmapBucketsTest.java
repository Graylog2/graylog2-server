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
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20261020120000_NullValuesForEmptyHeatmapBucketsTest {
    private final V20261020120000_NullValuesForEmptyHeatmapBuckets migration;
    private final MongoCollection<Document> searches;
    private final ClusterConfigService clusterConfigService;

    V20261020120000_NullValuesForEmptyHeatmapBucketsTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        this.searches = mongoDBTestService.mongoConnection().getMongoDatabase().getCollection("searches");
        this.migration = new V20261020120000_NullValuesForEmptyHeatmapBuckets(mongoDBTestService.mongoConnection(), clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }

    @Test
    void doesNothingWithoutViews() {
        migration.upgrade();

        assertThat(migrationCompleted().modifiedSearches()).isZero();
    }

    @Test
    @MongoDBFixtures("V20261020120000_NullValuesForEmptyHeatmapBucketsTest.json")
    void enablesNullValuesForHeatmapsOnly() {
        migration.upgrade();

        assertThat(migrationCompleted().modifiedSearches()).isEqualTo(1);

        final List<Document> searchTypes = searchTypes("6714c0a1c87c8c15c478fb10");
        final Document heatmap = searchTypes.get(0);
        assertThat(heatmap.getList("column_groups", Document.class).get(0).getBoolean("null_values_for_empty_buckets")).isTrue();
        assertThat(heatmap.getList("row_groups", Document.class).get(0).containsKey("null_values_for_empty_buckets")).isFalse();

        final Document barChart = searchTypes.get(1);
        assertThat(barChart.getList("row_groups", Document.class).get(0).containsKey("null_values_for_empty_buckets")).isFalse();

        final Document lineChart = searchTypes("6714c0a1c87c8c15c478fb11").get(0);
        assertThat(lineChart.getList("row_groups", Document.class).get(0).containsKey("null_values_for_empty_buckets")).isFalse();
    }

    @Test
    void doesNotRunTwice() {
        when(clusterConfigService.get(V20261020120000_NullValuesForEmptyHeatmapBuckets.MigrationCompleted.class))
                .thenReturn(new V20261020120000_NullValuesForEmptyHeatmapBuckets.MigrationCompleted(1, 0));

        migration.upgrade();

        verify(clusterConfigService, never()).write(any());
    }

    private List<Document> searchTypes(String searchId) {
        final Document search = searches.find(Filters.eq("_id", new ObjectId(searchId))).first();
        assertThat(search).isNotNull();

        return search.getList("queries", Document.class).get(0).getList("search_types", Document.class);
    }

    private V20261020120000_NullValuesForEmptyHeatmapBuckets.MigrationCompleted migrationCompleted() {
        final ArgumentCaptor<V20261020120000_NullValuesForEmptyHeatmapBuckets.MigrationCompleted> captor =
                ArgumentCaptor.forClass(V20261020120000_NullValuesForEmptyHeatmapBuckets.MigrationCompleted.class);
        verify(clusterConfigService).write(captor.capture());

        return captor.getValue();
    }
}
