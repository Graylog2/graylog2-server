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

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.datanode.Configuration;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.expr;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.currentDate;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.setOnInsert;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Makes sure that exactly one data node bootstraps a new opensearch cluster. Data nodes starting at the same time
 * can't see each other yet, if each of them bootstrapped a cluster with itself as the only manager node, they would
 * form independent clusters (split brain) that can never be merged.
 * <p>
 * The first node to start opensearch claims the bootstrap by an atomic write into MongoDB and is the only one
 * configuring {@code cluster.initial_cluster_manager_nodes}. All other nodes join it via discovery. The claim is a lease,
 * renewed while the claiming node's opensearch is running. If it isn't renewed, another node takes over the bootstrap.
 * As soon as the cluster is formed, its UUID is recorded and no other node will ever bootstrap again.
 */
@Singleton
public class ClusterBootstrapService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterBootstrapService.class);

    public static final String COLLECTION_NAME = "datanode_cluster_bootstrap";

    static final String BOOTSTRAP_ID = "bootstrap";
    static final String FIELD_ID = "_id";
    static final String FIELD_NODE_ID = "node_id";
    static final String FIELD_HOSTNAME = "hostname";
    static final String FIELD_CLAIMED_AT = "claimed_at";
    static final String FIELD_CLUSTER_UUID = "cluster_uuid";
    static final String FIELD_INITIAL_CLUSTER_MANAGER_NODES = "initial_cluster_manager_nodes";

    /**
     * Opensearch reports this UUID until a cluster has been formed
     */
    private static final String UNKNOWN_CLUSTER_UUID = "_na_";

    /**
     * A claim can't be taken over until it hasn't been renewed for this time. The claiming node renews it as long as its
     * opensearch process is running, see {@link #renewClaim()}.
     */
    private static final Duration DEFAULT_TAKEOVER_GRACE_PERIOD = Duration.ofSeconds(60);

    private final MongoCollection<Document> collection;
    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;
    private final Configuration configuration;
    private final Duration takeoverGracePeriod;

    private volatile boolean claimHeld;
    private volatile String verifiedClusterUuid;
    private volatile String clusterUuidMismatchWarning;

    @Inject
    public ClusterBootstrapService(MongoConnection mongoConnection, NodeService<DataNodeDto> nodeService, NodeId nodeId, Configuration configuration) {
        this(mongoConnection, nodeService, nodeId, configuration, DEFAULT_TAKEOVER_GRACE_PERIOD);
    }

    @VisibleForTesting
    ClusterBootstrapService(MongoConnection mongoConnection, NodeService<DataNodeDto> nodeService, NodeId nodeId, Configuration configuration, Duration takeoverGracePeriod) {
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.configuration = configuration;
        this.takeoverGracePeriod = takeoverGracePeriod;
    }

    /**
     * @return true if this node is the one that should bootstrap a new cluster, false if it should join an existing one.
     */
    public synchronized boolean claimBootstrap() {
        claimHeld = tryClaim();
        return claimHeld;
    }

    /**
     * Keeps the claim of this node alive while its opensearch process is running and may still bootstrap the cluster.
     * Without renewals, the claim expires and another node takes over the bootstrap.
     */
    public synchronized void renewClaim() {
        if (claimHeld && !refreshClaim(eq(FIELD_NODE_ID, nodeId.getNodeId()))) {
            LOG.debug("Opensearch cluster has been formed, the bootstrap claim doesn't need renewals anymore");
            claimHeld = false;
        }
    }

    private boolean tryClaim() {
        final Document claim = collection.find(eq(FIELD_ID, BOOTSTRAP_ID)).first();

        if (claim == null) {
            if (isAnyOtherNodeAvailable()) {
                // cluster formed before bootstrap claims existed, it will record its UUID soon. Never bootstrap next to it.
                LOG.info("Other data nodes are already available, joining their cluster instead of bootstrapping a new one");
                return false;
            }
            if (insertClaim(claimUpdate())) {
                LOG.info("This data node claimed the bootstrap of a new opensearch cluster");
                return true;
            }
            // someone else has been faster
            return tryClaim();
        }

        final String clusterUuid = claim.getString(FIELD_CLUSTER_UUID);
        if (clusterUuid != null) {
            LOG.info("Opensearch cluster {} has already been bootstrapped, joining it. If this cluster doesn't exist anymore, " +
                    "remove the document from the MongoDB collection {} or configure initial_cluster_manager_nodes explicitly.", clusterUuid, COLLECTION_NAME);
            return false;
        }

        final String explicitManagerNodes = claim.getString(FIELD_INITIAL_CLUSTER_MANAGER_NODES);
        if (explicitManagerNodes != null) {
            // explicitly configured bootstraps never expire, opensearch on these nodes bootstraps regardless of any claim
            LOG.info("Data node {} ({}) bootstraps the opensearch cluster with explicitly configured initial_cluster_manager_nodes {}, joining it",
                    claim.getString(FIELD_NODE_ID), claim.getString(FIELD_HOSTNAME), explicitManagerNodes);
            return false;
        }

        if (isClaimedByThisNode(claim)) {
            // restart before the cluster has been formed, refresh the claim unless it has been taken over or the cluster formed meanwhile
            if (refreshClaim(eq(FIELD_NODE_ID, nodeId.getNodeId()))) {
                LOG.info("This data node claimed the bootstrap of a new opensearch cluster");
                return true;
            }
            return tryClaim();
        }

        if (takeOverIfExpired(claim)) {
            return true;
        }
        LOG.info("Waiting for data node {} ({}) to bootstrap the opensearch cluster", claim.getString(FIELD_NODE_ID), claim.getString(FIELD_HOSTNAME));
        return false;
    }

    /**
     * Takes over the expired claim of another node while the cluster hasn't been formed yet. Called periodically by nodes
     * whose opensearch is already running and waiting to join, they wouldn't re-evaluate the claim otherwise.
     *
     * @return true if this node took over the bootstrap and has to restart opensearch with the bootstrap configuration
     */
    public synchronized boolean takeOverExpiredClaim() {
        if (claimHeld || verifiedClusterUuid != null) {
            return false;
        }
        final Document claim = collection.find(eq(FIELD_ID, BOOTSTRAP_ID)).first();
        if (claim == null || claim.getString(FIELD_CLUSTER_UUID) != null || claim.getString(FIELD_INITIAL_CLUSTER_MANAGER_NODES) != null || isClaimedByThisNode(claim)) {
            return false;
        }
        claimHeld = takeOverIfExpired(claim);
        return claimHeld;
    }

    /**
     * Registers the bootstrap of a node with explicitly configured {@code initial_cluster_manager_nodes}. Opensearch on
     * such a node bootstraps the cluster on its own, the claim only makes sure that no other node bootstraps a separate one.
     */
    public synchronized void registerExplicitBootstrap(String initialClusterManagerNodes) {
        if (insertClaim(combine(claimUpdate(), set(FIELD_INITIAL_CLUSTER_MANAGER_NODES, initialClusterManagerNodes)))) {
            LOG.info("Registered the opensearch cluster bootstrap with explicitly configured initial_cluster_manager_nodes {}", initialClusterManagerNodes);
            return;
        }

        final Document claim = collection.find(eq(FIELD_ID, BOOTSTRAP_ID)).first();
        if (claim != null && claim.getString(FIELD_CLUSTER_UUID) == null && claim.getString(FIELD_INITIAL_CLUSTER_MANAGER_NODES) == null) {
            LOG.warn("Data node {} ({}) is bootstrapping the opensearch cluster automatically, but this node bootstraps with explicitly configured " +
                            "initial_cluster_manager_nodes {}. They may form separate clusters. Configure initial_cluster_manager_nodes on all data nodes or on none of them.",
                    claim.getString(FIELD_NODE_ID), claim.getString(FIELD_HOSTNAME), initialClusterManagerNodes);
        }
    }

    /**
     * Record the UUID of the cluster this node is part of. The first recorded UUID marks the cluster as formed,
     * any node reporting a different UUID is not part of that cluster.
     */
    public synchronized void recordClusterUuid(String clusterUuid) {
        if (clusterUuid == null || clusterUuid.isBlank() || UNKNOWN_CLUSTER_UUID.equals(clusterUuid) || clusterUuid.equals(verifiedClusterUuid)) {
            return;
        }

        try {
            collection.updateOne(
                    and(eq(FIELD_ID, BOOTSTRAP_ID), eq(FIELD_CLUSTER_UUID, null)),
                    combine(
                            set(FIELD_CLUSTER_UUID, clusterUuid),
                            setOnInsert(FIELD_NODE_ID, nodeId.getNodeId()),
                            setOnInsert(FIELD_HOSTNAME, configuration.getHostname()),
                            currentDate(FIELD_CLAIMED_AT)
                    ),
                    new UpdateOptions().upsert(true));
        } catch (MongoException e) {
            if (!MongoUtils.isDuplicateKeyError(e)) {
                throw e;
            }
            // a cluster UUID has already been recorded, verify it below
        }

        final String recordedClusterUuid = Optional.ofNullable(collection.find(eq(FIELD_ID, BOOTSTRAP_ID)).first())
                .map(d -> d.getString(FIELD_CLUSTER_UUID))
                .orElse(null);

        if (clusterUuid.equals(recordedClusterUuid)) {
            if (clusterUuidMismatchWarning != null) {
                LOG.info("This data node is now part of the opensearch cluster {}", clusterUuid);
            }
            verifiedClusterUuid = clusterUuid;
            clusterUuidMismatchWarning = null;
        } else {
            final String warning = f("This data node formed or joined the opensearch cluster %s, but the data node cluster has been bootstrapped as %s. " +
                    "The node is not part of the data node cluster. Stop the node, remove or detach its opensearch data directory and start it again.", clusterUuid, recordedClusterUuid);
            if (!warning.equals(clusterUuidMismatchWarning)) {
                LOG.error(warning);
            }
            clusterUuidMismatchWarning = warning;
        }
    }

    public List<String> warnings() {
        final String warning = clusterUuidMismatchWarning;
        return warning == null ? List.of() : List.of(warning);
    }

    /**
     * @return true if the claim has been created, false if another node created it first or the cluster has been formed
     */
    private boolean insertClaim(Bson update) {
        try {
            // the conditions never match a claim of another node or a formed cluster, the upsert fails on the duplicate _id instead
            collection.updateOne(
                    and(eq(FIELD_ID, BOOTSTRAP_ID), eq(FIELD_NODE_ID, nodeId.getNodeId()), eq(FIELD_CLUSTER_UUID, null)),
                    update,
                    new UpdateOptions().upsert(true));
            return true;
        } catch (MongoException e) {
            if (MongoUtils.isDuplicateKeyError(e)) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Assigns the claim to this node and renews its timestamp, as long as the cluster hasn't been formed yet.
     *
     * @return true if the claim matching the condition has been updated
     */
    private boolean refreshClaim(Bson condition) {
        return collection.updateOne(
                and(eq(FIELD_ID, BOOTSTRAP_ID), eq(FIELD_CLUSTER_UUID, null), condition),
                claimUpdate()
        ).getMatchedCount() > 0;
    }

    private boolean takeOverIfExpired(Document claim) {
        final String claimant = claim.getString(FIELD_NODE_ID);
        final Bson claimExpired = expr(new Document("$lt", List.of(
                "$" + FIELD_CLAIMED_AT,
                new Document("$subtract", List.of("$$NOW", takeoverGracePeriod.toMillis()))
        )));

        if (refreshClaim(and(eq(FIELD_NODE_ID, claimant), claimExpired))) {
            LOG.warn("Data node {} claimed the opensearch cluster bootstrap but stopped renewing its claim, taking over the bootstrap", claimant);
            return true;
        }
        return false;
    }

    private Bson claimUpdate() {
        return combine(
                set(FIELD_NODE_ID, nodeId.getNodeId()),
                set(FIELD_HOSTNAME, configuration.getHostname()),
                currentDate(FIELD_CLAIMED_AT)
        );
    }

    private boolean isClaimedByThisNode(Document claim) {
        return nodeId.getNodeId().equals(claim.getString(FIELD_NODE_ID));
    }

    private boolean isAnyOtherNodeAvailable() {
        return nodeService.allActive().entrySet().stream()
                .filter(e -> !e.getKey().equals(nodeId.getNodeId()))
                .map(Map.Entry::getValue)
                .anyMatch(n -> n.getDataNodeStatus() == DataNodeStatus.AVAILABLE);
    }
}
