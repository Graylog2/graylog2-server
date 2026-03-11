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
package org.graylog2.cluster;

import com.mongodb.DBCollection;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.Configuration;
import org.graylog2.cluster.nodes.ServerNodeClusterService;
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

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class NodeServiceImplTest {
    public static final int STALE_LEADER_TIMEOUT_MS = 2000;

    private static final URI TRANSPORT_URI = URI.create("http://10.0.0.1:12900");
    private static final String LOCAL_CANONICAL_HOSTNAME = Tools.getLocalCanonicalHostname();
    private static final String NODE_ID = "28164cbe-4ad9-4c9c-a76e-088655aa7889";
    private static final Lifecycle LIFECYCLE = Lifecycle.RUNNING;
    private static final boolean IS_PROCESSING = true;

    @Mock
    private Configuration configuration;
    private final NodeId nodeId = new SimpleNodeId(NODE_ID);

    private NodeService nodeService;
    private MongoCollections mongoCollections;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) throws Exception {
        this.mongoCollections = mongoCollections;
        Mockito.when(configuration.getStaleLeaderTimeout()).thenReturn(STALE_LEADER_TIMEOUT_MS);
        this.nodeService = new NodeServiceImpl(
                new ServerNodeClusterService(mongoCollections.mongoConnection(), configuration));
    }

    @Test
    @MongoDBFixtures("NodeServiceImplTest-empty.json")
    public void testRegisterServer() throws Exception {
        assertThat(nodeService.allActive())
                .describedAs("The collection should be empty")
                .isEmpty();

        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME, IS_PROCESSING, LIFECYCLE);

        final Node node = nodeService.byNodeId(nodeId);

        assertThat(node).isNotNull();
        assertThat(node.getHostname()).isEqualTo(LOCAL_CANONICAL_HOSTNAME);
        assertThat(node.getTransportAddress()).isEqualTo(TRANSPORT_URI.toString());
        assertThat(node.isLeader()).isTrue();
    }

    @Test
    @MongoDBFixtures("NodeServiceImplTest-one-node.json")
    public void testRegisterServerWithExistingNode() throws Exception {
        final Node node1 = nodeService.byNodeId(nodeId);

        assertThat(node1.getNodeId())
                .describedAs("There should be one existing node")
                .isEqualTo(NODE_ID);

        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME, IS_PROCESSING, LIFECYCLE);

        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoCollections.mongoConnection().getDatabase().getCollection("nodes");

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
        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME, IS_PROCESSING, LIFECYCLE);
        assertThat(nodeService.allActive().keySet()).containsExactly(nodeId.getNodeId());

    }

}
