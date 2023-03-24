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
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeServiceImplTest {
    public static final int STALE_LEADER_TIMEOUT_MS = 2000;
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private static final URI TRANSPORT_URI = URI.create("http://10.0.0.1:12900");
    private static final String LOCAL_CANONICAL_HOSTNAME = Tools.getLocalCanonicalHostname();
    private static final String NODE_ID = "28164cbe-4ad9-4c9c-a76e-088655aa7889";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Configuration configuration;
    private final NodeId nodeId = new SimpleNodeId(NODE_ID);

    private NodeService nodeService;

    @Before
    public void setUp() throws Exception {
        Mockito.when(configuration.getStaleLeaderTimeout()).thenReturn(STALE_LEADER_TIMEOUT_MS);
        this.nodeService = new NodeServiceImpl(mongodb.mongoConnection(), configuration);
    }

    @Test
    @MongoDBFixtures("NodeServiceImplTest-empty.json")
    public void testRegisterServer() throws Exception {
        assertThat(nodeService.allActive())
                .describedAs("The collection should be empty")
                .isEmpty();

        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME);

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

        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME);

        @SuppressWarnings("deprecation")
        final DBCollection collection = mongodb.mongoConnection().getDatabase().getCollection("nodes");

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
    public void testMarkAsAlive() throws NodeNotFoundException {
        assertThat(nodeService.allActive())
                .describedAs("The collection should be empty")
                .isEmpty();

        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME);
        nodeService.markAsAlive(nodeId, false, URI.create("http://10.0.0.1:12901"));

        final Node node = nodeService.byNodeId(nodeId);

        assertThat(node.isLeader()).isFalse();
        assertThat(node.getTransportAddress()).isEqualTo("http://10.0.0.1:12901");
        assertThat(node.getLastSeen()).isNotNull().isInstanceOf(DateTime.class);
    }

    @Test
    public void testAllActive() throws NodeNotFoundException {
        assertThat(nodeService.allActive().keySet()).isEmpty();
        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME);
        assertThat(nodeService.allActive().keySet()).containsExactly(nodeId.getNodeId());

        nodeService.markAsAlive(nodeId, false, TRANSPORT_URI);
        assertThat(nodeService.allActive().keySet()).containsExactly(nodeId.getNodeId());
    }

    @Test
    public void testLastSeenBackwardsCompatibility() throws NodeNotFoundException, ValidationException {
        nodeService.registerServer(nodeId.getNodeId(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME);
        final Node node = nodeService.byNodeId(nodeId);

        final long lastSeenMs = System.currentTimeMillis() - 2 * STALE_LEADER_TIMEOUT_MS;
        node.getFields().put("last_seen", (int)(lastSeenMs / 1000));
        nodeService.save(node);

        final Node nodeAfterUpdate = nodeService.byNodeId(nodeId);
        final long lastSeenFromDb = nodeAfterUpdate.getLastSeen().toInstant().getMillis();
        Assertions.assertThat(lastSeenMs - lastSeenFromDb).isLessThan(1000); // make sure that our lastSeen from int is the same valid date

        final Map<String, Node> activeNodes = nodeService.allActive();

        // the node is stale, should not be present here
        Assertions.assertThat(activeNodes).isEmpty();

        // this should drop the node with the int timestamp, as it's at least 2xstale_delay outdated.
        nodeService.dropOutdated();

        Assertions.assertThatThrownBy(() -> nodeService.byNodeId(nodeId))
                .isInstanceOf(NodeNotFoundException.class);
    }
}
