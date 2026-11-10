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

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.migrations.V20261015120000_RemoveLegacyDatanodeCertificatesCollection.LEGACY_COLLECTION_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20261015120000_RemoveLegacyDatanodeCertificatesCollectionTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    private Migration migration;
    private MongoDatabase database;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.database = mongodb.mongoConnection().getMongoDatabase();
        this.migration = new V20261015120000_RemoveLegacyDatanodeCertificatesCollection(mongodb.mongoConnection(), clusterConfigService);
    }

    @Test
    void dropsLegacyCollection() {
        database.getCollection(LEGACY_COLLECTION_NAME).insertOne(new Document("node_id", "datanode-1"));
        assertThat(collectionNames()).contains(LEGACY_COLLECTION_NAME);

        migration.upgrade();

        assertThat(collectionNames()).doesNotContain(LEGACY_COLLECTION_NAME);
        verify(clusterConfigService).write(new V20261015120000_RemoveLegacyDatanodeCertificatesCollection.MigrationCompleted());
    }

    @Test
    void succeedsWhenCollectionDoesNotExist() {
        migration.upgrade();

        assertThat(collectionNames()).doesNotContain(LEGACY_COLLECTION_NAME);
        verify(clusterConfigService).write(new V20261015120000_RemoveLegacyDatanodeCertificatesCollection.MigrationCompleted());
    }

    @Test
    void skipsWhenAlreadyCompleted() {
        database.getCollection(LEGACY_COLLECTION_NAME).insertOne(new Document("node_id", "datanode-1"));
        when(clusterConfigService.get(V20261015120000_RemoveLegacyDatanodeCertificatesCollection.MigrationCompleted.class))
                .thenReturn(new V20261015120000_RemoveLegacyDatanodeCertificatesCollection.MigrationCompleted());

        migration.upgrade();

        assertThat(collectionNames()).contains(LEGACY_COLLECTION_NAME);
        verify(clusterConfigService, never()).write(any());
    }

    private ArrayList<String> collectionNames() {
        return database.listCollectionNames().into(new ArrayList<>());
    }
}
