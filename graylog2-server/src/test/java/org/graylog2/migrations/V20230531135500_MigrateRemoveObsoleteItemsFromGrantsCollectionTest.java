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
import org.graylog.security.DBGrantService;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollectionTest {
    private final V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection migration;
    private final MongoCollection<Document> collection;

    private final ClusterConfigService clusterConfigService;

    public V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollectionTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection(DBGrantService.COLLECTION_NAME);
        migration = new V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection(mongoConnection, clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }

    @Test
    @MongoDBFixtures("V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollectionTest_noElements.json")
    void notMigratingAnythingIfNoElementsArePresent() {
        assertThat(this.collection.countDocuments()).isEqualTo(9);

        this.migration.upgrade();

        assertThat(migrationCompleted()).isNotNull();
        assertThat(this.collection.countDocuments()).isEqualTo(9);
    }

    @Test
    @MongoDBFixtures("V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollectionTests.json")
    void removingAllObsoleteEntries() {
        assertThat(this.collection.countDocuments()).isEqualTo(13);

        this.migration.upgrade();

        assertThat(migrationCompleted()).isNotNull();
        assertThat(this.collection.countDocuments()).isEqualTo(9);

        this.collection.find().forEach(d -> {
            assertThat(d.get("target").toString()).doesNotContain("favorite");
            assertThat(d.get("target").toString()).doesNotContain("last_opened");
        });
    }

    private V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection.MigrationCompleted migrationCompleted() {
        final ArgumentCaptor<V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection.MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor
                .forClass(V20230531135500_MigrateRemoveObsoleteItemsFromGrantsCollection.MigrationCompleted.class);
        verify(this.clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }
}
