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
package org.graylog2.cluster.nodes;

import com.mongodb.DBCollection;
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.Configuration;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ServerNodeClusterServiceTest {
    public static final int STALE_LEADER_TIMEOUT_MS = 2000;

    private static final URI TRANSPORT_URI = URI.create("http://10.0.0.1:12900");
    private static final String LOCAL_CANONICAL_HOSTNAME = Tools.getLocalCanonicalHostname();
    private static final String NODE_ID = "28164cbe-4ad9-4c9c-a76e-088655aa7889";
    private static final boolean IS_PROCESSING = true;
    private static final Lifecycle LIFECYCLE = Lifecycle.RUNNING;

    @Mock
    private Configuration configuration;
    private final NodeId nodeId = new SimpleNodeId(NODE_ID);

    private ServerNodeClusterService nodeService;
    private MongoCollections mongoCollections;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) throws Exception {
        this.mongoCollections = mongoCollections;
        Mockito.when(configuration.getStaleLeaderTimeout()).thenReturn(STALE_LEADER_TIMEOUT_MS);
        this.nodeService =
                new ServerNodeClusterService(mongoCollections, configuration);
    }

    @Test
    @MongoDBFixtures("ServerNodeClusterTest-empty.json")
    public void testRegisterServer() throws Exception {
        assertThat(nodeService.allActive())
                .describedAs("The collection should be empty")
                .isEmpty();

        nodeService.registerServer(ServerNodeDto.Builder.builder()
                .setId(nodeId.getNodeId())
                .setLeader(true)
                .setTransportAddress(TRANSPORT_URI.toString())
                .setHostname(LOCAL_CANONICAL_HOSTNAME)
                .setProcessing(IS_PROCESSING)
                .setLifecycle(LIFECYCLE)
                .build());

        final Node node = nodeService.byNodeId(nodeId);

        assertThat(node).isNotNull();
        assertThat(node.getHostname()).isEqualTo(LOCAL_CANONICAL_HOSTNAME);
        assertThat(node.getTransportAddress()).isEqualTo(TRANSPORT_URI.toString());
        assertThat(node.isLeader()).isTrue();
    }

    @Test
    @MongoDBFixtures("ServerNodeClusterTest-one-node.json")
    public void testRegisterServerWithExistingNode() throws Exception {
        final Node node1 = nodeService.byNodeId(nodeId);

        assertThat(node1.getNodeId())
                .describedAs("There should be one existing node")
                .isEqualTo(NODE_ID);

        nodeService.registerServer(ServerNodeDto.Builder.builder()
                .setId(nodeId.getNodeId())
                .setLeader(true)
                .setTransportAddress(TRANSPORT_URI.toString())
                .setHostname(LOCAL_CANONICAL_HOSTNAME)
                .setProcessing(IS_PROCESSING)
                .setLifecycle(LIFECYCLE)
                .build());

        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoCollections.connection().getDatabase().getCollection("nodes");

        assertThat(collection.count())
                .describedAs("There should only be one node")
                .isEqualTo(1);

        final Node node2 = nodeService.byNodeId(nodeId);

        assertThat(node2).isNotNull();
        assertThat(node2.getHostname()).isEqualTo(LOCAL_CANONICAL_HOSTNAME);
        assertThat(node2.getTransportAddress()).isEqualTo(TRANSPORT_URI.toString());
        assertThat(node2.isLeader()).isTrue();
    }

    @Test
    public void testAllActive() throws NodeNotFoundException {
        assertThat(nodeService.allActive().keySet()).isEmpty();
        nodeService.registerServer(ServerNodeDto.Builder.builder()
                .setId(nodeId.getNodeId())
                .setLeader(true)
                .setTransportAddress(TRANSPORT_URI.toString())
                .setHostname(LOCAL_CANONICAL_HOSTNAME)
                .setProcessing(IS_PROCESSING)
                .setLifecycle(LIFECYCLE)
                .build());
        assertThat(nodeService.allActive().keySet()).containsExactly(nodeId.getNodeId());

    }

    @Test
    public void testLastSeenBackwardsCompatibility() throws NodeNotFoundException {
        nodeService.registerServer(ServerNodeDto.Builder.builder()
                .setId(nodeId.getNodeId())
                .setLeader(true)
                .setTransportAddress(TRANSPORT_URI.toString())
                .setHostname(LOCAL_CANONICAL_HOSTNAME)
                .setProcessing(IS_PROCESSING)
                .setLifecycle(LIFECYCLE)
                .build());

        // Older Graylog versions wrote last_seen as numeric epoch seconds rather than a BSON Date.
        // Overwrite the field directly to simulate that on-disk shape.
        final long staleEpochSeconds = (System.currentTimeMillis() - 2L * STALE_LEADER_TIMEOUT_MS) / 1000L;
        mongoCollections.mongoConnection().getMongoDatabase()
                .getCollection(ServerNodeDto.COLLECTION_NAME)
                .updateOne(eq("node_id", nodeId.getNodeId()), set("last_seen", staleEpochSeconds));

        // byNodeId must deserialize the numeric value via LastSeenDeserializer.
        final Node nodeAfterUpdate = nodeService.byNodeId(nodeId);
        Assertions.assertThat(nodeAfterUpdate.getLastSeen().getMillis())
                .isEqualTo(staleEpochSeconds * 1000L);

        // The aggregation pipeline must coerce the numeric value to a Date and treat the node as stale.
        Assertions.assertThat(nodeService.allActive()).isEmpty();

        // dropOutdated must remove the legacy-formatted node.
        nodeService.dropOutdated();
        Assertions.assertThatThrownBy(() -> nodeService.byNodeId(nodeId))
                .isInstanceOf(NodeNotFoundException.class);
    }

}
