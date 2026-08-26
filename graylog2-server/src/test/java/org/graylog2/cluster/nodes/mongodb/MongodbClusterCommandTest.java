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
package org.graylog2.cluster.nodes.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongodbClusterCommandTest {

    private MongoConnection mongoConnection;
    private MongoClient mainClient;
    private MongodbConnectionResolver connectionResolver;

    @BeforeEach
    void setUp() {
        mongoConnection = mock(MongoConnection.class);
        mainClient = mock(MongoClient.class);
        connectionResolver = mock(MongodbConnectionResolver.class);
        when(mongoConnection.connect()).thenReturn(mainClient);
    }

    @Test
    void testExecutesOnAllHostsInReplicaSet() {
        List<String> hosts = List.of("host1:27017", "host2:27017", "host3:27017");
        setupReplicaSet(hosts);

        MongodbClusterCommand command = new MongodbClusterCommand(mongoConnection, connectionResolver);
        Map<String, String> results = command.runOnEachNode((host, client) -> "result-" + host);

        assertThat(results).hasSize(3)
                .containsKeys("host1:27017", "host2:27017", "host3:27017")
                .containsValues("result-host1:27017", "result-host2:27017", "result-host3:27017");
    }

    @Test
    void testTimeoutExcludesSlowHost() {
        List<String> hosts = List.of("fast:27017", "slow:27017");
        setupReplicaSet(hosts);

        MongodbClusterCommand command = new MongodbClusterCommand(mongoConnection, connectionResolver, 1);
        Map<String, String> results = command.runOnEachNode((host, client) -> {
            if (host.contains("slow")) {
                try {
                    Thread.sleep(2000); // Exceeds 1s timeout
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return "result-" + host;
        });

        assertThat(results).doesNotContainKey("slow:27017");
        assertThat(results).containsKey("fast:27017");
    }

    @Test
    void testStandaloneInstanceExecutesOnSingleHost() {
        setupStandalone("standalone:27017");

        MongodbClusterCommand command = new MongodbClusterCommand(mongoConnection, connectionResolver);
        AtomicInteger callCount = new AtomicInteger();
        Map<String, Integer> results = command.runOnEachNode((host, client) -> callCount.incrementAndGet());

        assertThat(results).hasSize(1).containsKey("standalone:27017");
        assertThat(callCount.get()).isEqualTo(1);
    }

    private void setupReplicaSet(List<String> hosts) {
        com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
        when(mainClient.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME)).thenReturn(mockDatabase);
        when(mockDatabase.runCommand(any(Document.class)))
                .thenReturn(new Document("hosts", hosts).append("ok", 1));

        hosts.forEach(host -> when(connectionResolver.resolve(host)).thenReturn(mock(MongoClient.class)));
    }

    private void setupStandalone(String hostAddress) {
        com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
        when(mainClient.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME)).thenReturn(mockDatabase);
        when(mockDatabase.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));

        ServerAddress address = new ServerAddress(hostAddress);
        ServerDescription serverDesc = ServerDescription.builder()
                .address(address)
                .state(com.mongodb.connection.ServerConnectionState.CONNECTED)
                .build();
        ClusterDescription clusterDesc = new ClusterDescription(
                com.mongodb.connection.ClusterConnectionMode.SINGLE,
                com.mongodb.connection.ClusterType.STANDALONE,
                List.of(serverDesc)
        );
        when(mainClient.getClusterDescription()).thenReturn(clusterDesc);
    }
}
