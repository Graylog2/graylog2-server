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
package org.graylog.collectors.migrations;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.collectors.CollectorsConfig;
import org.graylog2.database.MongoConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V20260316000000_MigrateCollectorsDataTest {
    private static final String CONFIG_TYPE = "org.graylog.collectors.CollectorsConfig";
    private static final String GRPC_INPUT_TYPE = "org.graylog.collectors.input.CollectorIngestGrpcInput";

    @Mock
    private MongoConnection mongoConnection;
    @Mock
    private MongoDatabase mongoDatabase;
    @Mock
    private MongoCollection<Document> clusterConfigCollection;
    @Mock
    private MongoCollection<Document> collectorInstancesCollection;
    @Mock
    private MongoCollection<Document> inputsCollection;
    @Mock
    private FindIterable<Document> configFindIterable;
    @Mock
    private FindIterable<Document> lastSeenFindIterable;
    @Mock
    private MongoCursor<Document> lastSeenCursor;

    private V20260316000000_MigrateCollectorsData migration;

    @BeforeEach
    void setUp() {
        migration = new V20260316000000_MigrateCollectorsData(mongoConnection);

        when(mongoConnection.getMongoDatabase()).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection("cluster_config")).thenReturn(clusterConfigCollection);
        when(mongoDatabase.getCollection("collector_instances")).thenReturn(collectorInstancesCollection);
        when(mongoDatabase.getCollection("inputs")).thenReturn(inputsCollection);

        when(clusterConfigCollection.find(any(Bson.class))).thenReturn(configFindIterable);
        when(collectorInstancesCollection.find(any(Bson.class))).thenReturn(lastSeenFindIterable);
        when(lastSeenFindIterable.iterator()).thenReturn(lastSeenCursor);
        when(lastSeenCursor.hasNext()).thenReturn(false);
        when(collectorInstancesCollection.updateMany(any(Bson.class), any(Bson.class)))
                .thenReturn(UpdateResult.acknowledged(0L, 0L, null));
        when(inputsCollection.deleteMany(any(Bson.class)))
                .thenReturn(DeleteResult.acknowledged(0L));
    }

    @Test
    void upgradeDeletesAllPersistedGrpcInputsAndUnsetsGrpcConfig() {
        when(configFindIterable.first()).thenReturn(new Document("type", CONFIG_TYPE)
                .append("payload", new Document("http", new Document("enabled", true)
                                .append("hostname", "example.org")
                                .append("port", 14401)
                                .append("input_id", "http-input"))
                        .append("grpc", new Document("enabled", true)
                                .append("hostname", "example.org")
                                .append("port", 14402)
                                .append("input_id", "grpc-input"))
                        .append("collector_offline_threshold", CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD.toString())
                        .append("collector_default_visibility_threshold", CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD.toString())
                        .append("collector_expiration_threshold", CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD.toString())));
        when(clusterConfigCollection.updateOne(any(Bson.class), any(Bson.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));
        migration.upgrade();

        final ArgumentCaptor<Bson> deleteCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(inputsCollection).deleteMany(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry()))
                .isEqualTo(new BsonDocument("type", new org.bson.BsonString(GRPC_INPUT_TYPE)));

        final ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(clusterConfigCollection).updateOne(any(Bson.class), updateCaptor.capture());
        assertThat(updateCaptor.getValue().toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry()))
                .isEqualTo(BsonDocument.parse("{\"$unset\": {\"payload.grpc\": \"\"}}"));
    }

    @Test
    void upgradeIsNoOpWhenGrpcConfigAndInputsAreMissing() {
        when(configFindIterable.first()).thenReturn(null);
        migration.upgrade();

        verify(inputsCollection).deleteMany(any(Bson.class));
        verify(clusterConfigCollection, never()).updateOne(any(Bson.class), any(Bson.class));
    }
}
