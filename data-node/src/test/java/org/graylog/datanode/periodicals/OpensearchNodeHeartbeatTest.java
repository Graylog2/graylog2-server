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
package org.graylog.datanode.periodicals;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.OpensearchStartRequestedEvent;
import org.graylog.datanode.opensearch.bootstrap.ClusterBootstrapService;
import org.graylog.datanode.opensearch.bootstrap.InitialClusterManagerNodesResolver;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.cluster.nodes.DataNodeClusterService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
class OpensearchNodeHeartbeatTest {

    private static final Date EXPIRED_CLAIM = new Date(0);
    // field names of the bootstrap document, see ClusterBootstrapService
    private static final Document BOOTSTRAP_FILTER = new Document("_id", "bootstrap");
    private static final String FIELD_NODE_ID = "node_id";
    private static final String FIELD_CLAIMED_AT = "claimed_at";

    @TempDir
    private Path tempDir;

    private MongoConnection mongoConnection;
    private DataNodeClusterService nodeService;
    private EventBus eventBus;
    private final List<OpensearchStartRequestedEvent> startRequests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoConnection = mongoCollections.connection();
        this.nodeService = new DataNodeClusterService(mongoCollections, new org.graylog2.Configuration());
        this.eventBus = new EventBus();
        this.eventBus.register(new Object() {
            @Subscribe
            public void onStartRequested(OpensearchStartRequestedEvent event) {
                startRequests.add(event);
            }
        });
    }

    @Test
    void claimantRenewsClaimWhileProcessIsRunning() throws Exception {
        final ClusterBootstrapService node1 = bootstrapService("node1");
        assertThat(node1.claimBootstrap()).isTrue();

        // the REST API isn't responding, but the process is still running and may bootstrap the cluster
        expireClaim();
        heartbeat(node1, OpensearchState.FAILED).doRun();
        assertThat(claimedAt()).isAfter(EXPIRED_CLAIM);

        // the process isn't running, the claim has to expire
        expireClaim();
        heartbeat(node1, OpensearchState.PREPARED).doRun();
        assertThat(claimedAt()).isEqualTo(EXPIRED_CLAIM);
    }

    @Test
    void waitingNodeTakesOverExpiredClaimAndRestarts() throws Exception {
        assertThat(bootstrapService("node1").claimBootstrap()).isTrue();
        final ClusterBootstrapService node2 = bootstrapService("node2");
        assertThat(node2.claimBootstrap()).isFalse();

        // node1 renews its claim, node2 keeps waiting
        final OpensearchNodeHeartbeat heartbeat = heartbeat(node2, OpensearchState.STARTING);
        heartbeat.doRun();
        assertThat(startRequests).isEmpty();
        assertThat(claimant()).isEqualTo("node1");

        // node1 is gone, node2 takes over and restarts its opensearch to bootstrap the cluster
        expireClaim();
        heartbeat.doRun();
        assertThat(startRequests).hasSize(1);
        assertThat(claimant()).isEqualTo("node2");

        // node2 holds the claim now and only renews it
        heartbeat.doRun();
        assertThat(startRequests).hasSize(1);
    }

    @Test
    void noTakeoverIfProcessCantBeRestarted() throws Exception {
        assertThat(bootstrapService("node1").claimBootstrap()).isTrue();
        final ClusterBootstrapService node2 = bootstrapService("node2");
        expireClaim();

        // there's no transition from NOT_RESPONDING to a restart, node2 would block the bootstrap with a claim it can't use
        heartbeat(node2, OpensearchState.NOT_RESPONDING).doRun();
        // PREPARED isn't running at all
        heartbeat(node2, OpensearchState.PREPARED).doRun();

        assertThat(startRequests).isEmpty();
        assertThat(claimant()).isEqualTo("node1");
    }

    @Test
    void noTakeoverByNodeWithExplicitConfiguration() throws Exception {
        assertThat(bootstrapService("node1").claimBootstrap()).isTrue();
        final Configuration configuration = configuration("node2", Map.of("initial_cluster_manager_nodes", "node2,node3"));
        final ClusterBootstrapService node2 = bootstrapService("node2", configuration);
        expireClaim();

        heartbeat(node2, configuration, OpensearchState.STARTING).doRun();

        assertThat(startRequests).isEmpty();
        assertThat(claimant()).isEqualTo("node1");
    }

    private OpensearchNodeHeartbeat heartbeat(ClusterBootstrapService bootstrapService, OpensearchState state) throws Exception {
        return heartbeat(bootstrapService, configuration("unused", Map.of()), state);
    }

    private OpensearchNodeHeartbeat heartbeat(ClusterBootstrapService bootstrapService, Configuration configuration, OpensearchState state) {
        final OpensearchProcess process = mock(OpensearchProcess.class);
        // no opensearch client, the heartbeat doesn't call the REST API
        when(process.isInState(any())).thenAnswer(invocation -> invocation.getArgument(0) == state);
        return new OpensearchNodeHeartbeat(process, bootstrapService, new InitialClusterManagerNodesResolver(configuration, bootstrapService), eventBus);
    }

    private ClusterBootstrapService bootstrapService(String nodeId) throws Exception {
        return bootstrapService(nodeId, configuration(nodeId, Map.of()));
    }

    private ClusterBootstrapService bootstrapService(String nodeId, Configuration configuration) {
        return new ClusterBootstrapService(mongoConnection, nodeService, new SimpleNodeId(nodeId), configuration);
    }

    private Configuration configuration(String nodeId, Map<String, String> properties) throws Exception {
        final Map<String, String> withHostname = new HashMap<>(properties);
        withHostname.put("hostname", nodeId + ".example.com");
        return DatanodeTestUtils.datanodeConfiguration(withHostname, Files.createTempDirectory(tempDir, nodeId));
    }

    private void expireClaim() {
        bootstrapCollection().updateOne(BOOTSTRAP_FILTER, new Document("$set", new Document(FIELD_CLAIMED_AT, EXPIRED_CLAIM)));
    }

    private Date claimedAt() {
        return bootstrapCollection().find(BOOTSTRAP_FILTER).first().getDate(FIELD_CLAIMED_AT);
    }

    private String claimant() {
        return bootstrapCollection().find(BOOTSTRAP_FILTER).first().getString(FIELD_NODE_ID);
    }

    private MongoCollection<Document> bootstrapCollection() {
        return mongoConnection.getMongoDatabase().getCollection(ClusterBootstrapService.COLLECTION_NAME);
    }
}
