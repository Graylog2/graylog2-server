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
package org.graylog.datanode.opensearch.bootstrap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bson.Document;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.cluster.nodes.DataNodeClusterService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class ClusterBootstrapServiceTest {

    private static final Duration GRACE_PERIOD = Duration.ofMinutes(10);
    // a negative grace period lets every claim expire immediately, without depending on the clock
    private static final Duration EXPIRED_GRACE_PERIOD = Duration.ofMinutes(-1);
    private static final Date EXPIRED_CLAIM = new Date(0);

    @TempDir
    private Path tempDir;

    private MongoConnection mongoConnection;
    private DataNodeClusterService nodeService;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoConnection = mongoCollections.connection();
        this.nodeService = new DataNodeClusterService(mongoCollections, new org.graylog2.Configuration());
    }

    @Test
    void firstNodeClaimsBootstrap() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        final ClusterBootstrapService node2 = service("node2", GRACE_PERIOD);

        assertThat(node1.claimBootstrap()).isTrue();
        // node1 hasn't even registered yet, the grace period protects its claim
        assertThat(node2.claimBootstrap()).isFalse();

        register("node1", DataNodeStatus.STARTING);
        assertThat(node2.claimBootstrap()).isFalse();

        // restart of node1 before the cluster has been formed keeps its claim
        assertThat(node1.claimBootstrap()).isTrue();
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node1");
    }

    @Test
    void onlyOneOfConcurrentlyStartingNodesClaimsBootstrap() throws Exception {
        final int nodes = 10;
        final CountDownLatch start = new CountDownLatch(1);
        final List<Callable<Boolean>> claims = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            final ClusterBootstrapService service = service("node" + i, GRACE_PERIOD);
            claims.add(() -> {
                start.await();
                return service.claimBootstrap();
            });
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(nodes, new ThreadFactoryBuilder().setNameFormat("bootstrap-claim-%d").build())) {
            final List<Future<Boolean>> results = claims.stream().map(executor::submit).toList();
            start.countDown();
            long winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
        }
    }

    @Test
    void claimNotRenewedIsTakenOverRegardlessOfStatus() throws Exception {
        assertThat(service("node1", EXPIRED_GRACE_PERIOD).claimBootstrap()).isTrue();

        // node1 still reports its opensearch as available, but stopped renewing its claim
        register("node1", DataNodeStatus.AVAILABLE);
        assertThat(service("node2", EXPIRED_GRACE_PERIOD).claimBootstrap()).isTrue();
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node2");

        // node1 comes back while node2's claim is fresh, it has to join node2 now
        assertThat(service("node1", GRACE_PERIOD).claimBootstrap()).isFalse();
    }

    @Test
    void renewedClaimIsNotTakenOver() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        assertThat(node1.claimBootstrap()).isTrue();

        // node1's opensearch isn't responding, but its process is running and renews the claim
        register("node1", DataNodeStatus.UNAVAILABLE);
        expireClaim();
        node1.renewClaim();
        assertThat(service("node2", GRACE_PERIOD).claimBootstrap()).isFalse();

        // node1 stopped renewing
        expireClaim();
        assertThat(service("node2", GRACE_PERIOD).claimBootstrap()).isTrue();

        // late renewal of node1 doesn't take the claim back
        node1.renewClaim();
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node2");
    }

    @Test
    void onlyClaimantRenewsUntilClusterFormed() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        final ClusterBootstrapService node2 = service("node2", GRACE_PERIOD);
        assertThat(node1.claimBootstrap()).isTrue();
        assertThat(node2.claimBootstrap()).isFalse();

        expireClaim();
        node2.renewClaim();
        assertThat(bootstrapDocument().getDate(ClusterBootstrapService.FIELD_CLAIMED_AT)).isEqualTo(EXPIRED_CLAIM);

        node1.recordClusterUuid("cluster-a");
        expireClaim();
        node1.renewClaim();
        assertThat(bootstrapDocument().getDate(ClusterBootstrapService.FIELD_CLAIMED_AT)).isEqualTo(EXPIRED_CLAIM);
    }

    @Test
    void claimOfUnregisteredNodeIsTakenOverAfterGracePeriod() throws Exception {
        assertThat(service("node1", GRACE_PERIOD).claimBootstrap()).isTrue();
        assertThat(service("node2", GRACE_PERIOD).claimBootstrap()).isFalse();
        assertThat(service("node2", EXPIRED_GRACE_PERIOD).claimBootstrap()).isTrue();
    }

    @Test
    void waitingNodeTakesOverExpiredClaim() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        final ClusterBootstrapService node2 = service("node2", GRACE_PERIOD);
        final ClusterBootstrapService node3 = service("node3", GRACE_PERIOD);
        assertThat(node1.claimBootstrap()).isTrue();
        assertThat(node2.claimBootstrap()).isFalse();
        assertThat(node3.claimBootstrap()).isFalse();

        // node1 is renewing its claim
        assertThat(node1.takeOverExpiredClaim()).isFalse();
        assertThat(node2.takeOverExpiredClaim()).isFalse();

        // node1 is gone, only one of the waiting nodes takes over
        expireClaim();
        assertThat(node2.takeOverExpiredClaim()).isTrue();
        assertThat(node3.takeOverExpiredClaim()).isFalse();
        assertThat(node2.takeOverExpiredClaim()).isFalse();
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node2");

        // the configuration of the restarted node2 bootstraps the cluster, node1 has to join it
        assertThat(node2.claimBootstrap()).isTrue();
        node1.renewClaim();
        assertThat(service("node1", GRACE_PERIOD).claimBootstrap()).isFalse();
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node2");
    }

    @Test
    void noTakeoverOfFormedClusterOrExplicitBootstrap() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        node1.registerExplicitBootstrap("node1,node3");
        expireClaim();
        assertThat(service("node2", GRACE_PERIOD).takeOverExpiredClaim()).isFalse();

        node1.recordClusterUuid("cluster-a");
        expireClaim();
        assertThat(service("node2", GRACE_PERIOD).takeOverExpiredClaim()).isFalse();
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node1");
    }

    @Test
    void explicitBootstrapIsJoinedAndNeverTakenOver() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        node1.registerExplicitBootstrap("node1,node2");
        // nodes with the same explicit configuration don't overwrite the first registration
        service("node2", GRACE_PERIOD).registerExplicitBootstrap("node1,node2");

        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node1");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_INITIAL_CLUSTER_MANAGER_NODES)).isEqualTo("node1,node2");

        // explicit registrations are never renewed, but never expire either
        assertThat(service("node3", EXPIRED_GRACE_PERIOD).claimBootstrap()).isFalse();
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node1");

        node1.recordClusterUuid("cluster-a");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_CLUSTER_UUID)).isEqualTo("cluster-a");
    }

    @Test
    void explicitBootstrapDoesNotReplaceAutomaticClaim() throws Exception {
        assertThat(service("node1", GRACE_PERIOD).claimBootstrap()).isTrue();

        // too late, node1 already bootstraps on its own, only a warning is logged
        service("node2", GRACE_PERIOD).registerExplicitBootstrap("node2,node3");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_NODE_ID)).isEqualTo("node1");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_INITIAL_CLUSTER_MANAGER_NODES)).isNull();
    }

    @Test
    void explicitBootstrapDoesNotChangeFormedCluster() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        assertThat(node1.claimBootstrap()).isTrue();
        node1.recordClusterUuid("cluster-a");

        node1.registerExplicitBootstrap("node1,node2");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_INITIAL_CLUSTER_MANAGER_NODES)).isNull();
    }

    @Test
    void noBootstrapAfterClusterFormed() throws Exception {
        final ClusterBootstrapService node1 = service("node1", EXPIRED_GRACE_PERIOD);
        assertThat(node1.claimBootstrap()).isTrue();

        // opensearch reports _na_ until the cluster is formed
        node1.recordClusterUuid("_na_");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_CLUSTER_UUID)).isNull();

        node1.recordClusterUuid("cluster-a");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_CLUSTER_UUID)).isEqualTo("cluster-a");

        // neither the original node (e.g. with wiped data directory) nor any other node bootstraps again,
        // even if the claiming node is gone and the grace period expired
        assertThat(node1.claimBootstrap()).isFalse();
        assertThat(service("node2", EXPIRED_GRACE_PERIOD).claimBootstrap()).isFalse();
    }

    @Test
    void existingClusterWithoutClaimIsJoined() throws Exception {
        // cluster formed before bootstrap claims existed
        register("node1", DataNodeStatus.AVAILABLE);

        assertThat(service("node2", GRACE_PERIOD).claimBootstrap()).isFalse();
        assertThat(bootstrapDocument()).isNull();

        // the running cluster records its UUID, even without any previous claim
        service("node1", GRACE_PERIOD).recordClusterUuid("cluster-a");
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_CLUSTER_UUID)).isEqualTo("cluster-a");
        assertThat(service("node3", EXPIRED_GRACE_PERIOD).claimBootstrap()).isFalse();
    }

    @Test
    void reportsNodeOutsideOfBootstrappedCluster() throws Exception {
        final ClusterBootstrapService node1 = service("node1", GRACE_PERIOD);
        final ClusterBootstrapService node2 = service("node2", GRACE_PERIOD);

        node1.claimBootstrap();
        node1.recordClusterUuid("cluster-a");
        node2.recordClusterUuid("cluster-b");

        assertThat(node1.warnings()).isEmpty();
        assertThat(node2.warnings()).singleElement().asString().contains("cluster-a", "cluster-b");
        // the first recorded UUID stays
        assertThat(bootstrapDocument().getString(ClusterBootstrapService.FIELD_CLUSTER_UUID)).isEqualTo("cluster-a");

        // node2 has been fixed and joined the right cluster
        node2.recordClusterUuid("cluster-a");
        assertThat(node2.warnings()).isEmpty();
    }

    private ClusterBootstrapService service(String nodeId, Duration gracePeriod) throws Exception {
        final Configuration configuration = DatanodeTestUtils.datanodeConfiguration(
                Map.of("hostname", nodeId + ".example.com"),
                Files.createTempDirectory(tempDir, nodeId));
        return new ClusterBootstrapService(mongoConnection, nodeService, new SimpleNodeId(nodeId), configuration, gracePeriod);
    }

    private void register(String nodeId, DataNodeStatus status) {
        nodeService.registerServer(DataNodeDto.builder()
                .setId(nodeId)
                .setHostname(nodeId + ".example.com")
                .setDataNodeStatus(status)
                .build());
    }

    private void expireClaim() {
        mongoConnection.getMongoDatabase()
                .getCollection(ClusterBootstrapService.COLLECTION_NAME)
                .updateOne(new Document(ClusterBootstrapService.FIELD_ID, ClusterBootstrapService.BOOTSTRAP_ID),
                        new Document("$set", new Document(ClusterBootstrapService.FIELD_CLAIMED_AT, EXPIRED_CLAIM)));
    }

    private Document bootstrapDocument() {
        return mongoConnection.getMongoDatabase()
                .getCollection(ClusterBootstrapService.COLLECTION_NAME)
                .find(new Document(ClusterBootstrapService.FIELD_ID, ClusterBootstrapService.BOOTSTRAP_ID))
                .first();
    }
}
