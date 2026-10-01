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
import java.util.Set;

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
 * configuring {@code cluster.initial_cluster_manager_nodes}. All other nodes join it via discovery. As soon as the
 * cluster is formed, its UUID is recorded and no other node will ever bootstrap again.
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

    /**
     * Opensearch reports this UUID until a cluster has been formed
     */
    private static final String UNKNOWN_CLUSTER_UUID = "_na_";

    /**
     * Statuses of a claiming node that may still bootstrap the cluster
     */
    private static final Set<DataNodeStatus> BOOTSTRAPPING_STATUSES = Set.of(DataNodeStatus.STARTING, DataNodeStatus.AVAILABLE);

    /**
     * A claim can't be taken over for this time, giving the claiming node enough time to register and start opensearch.
     */
    private static final Duration DEFAULT_TAKEOVER_GRACE_PERIOD = Duration.ofSeconds(60);

    private final MongoCollection<Document> collection;
    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;
    private final Configuration configuration;
    private final Duration takeoverGracePeriod;

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
        final Document claim = collection.find(eq(FIELD_ID, BOOTSTRAP_ID)).first();

        if (claim == null) {
            if (isAnyOtherNodeAvailable()) {
                // cluster formed before bootstrap claims existed, it will record its UUID soon. Never bootstrap next to it.
                LOG.info("Other data nodes are already available, joining their cluster instead of bootstrapping a new one");
                return false;
            }
            if (insertClaim()) {
                LOG.info("This data node claimed the bootstrap of a new opensearch cluster");
                return true;
            }
            // someone else has been faster
            return claimBootstrap();
        }

        final String clusterUuid = claim.getString(FIELD_CLUSTER_UUID);
        if (clusterUuid != null) {
            LOG.info("Opensearch cluster {} has already been bootstrapped, joining it. If this cluster doesn't exist anymore, " +
                    "remove the document from the MongoDB collection {} or configure initial_cluster_manager_nodes explicitly.", clusterUuid, COLLECTION_NAME);
            return false;
        }

        if (isClaimedByThisNode(claim)) {
            // restart before the cluster has been formed, refresh the claim unless it has been taken over or the cluster formed meanwhile
            if (refreshClaim(eq(FIELD_NODE_ID, nodeId.getNodeId()))) {
                LOG.info("This data node claimed the bootstrap of a new opensearch cluster");
                return true;
            }
            return claimBootstrap();
        }

        return takeOverIfClaimantGone(claim);
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
     * @return true if the claim has been created, false if another node created it first
     */
    private boolean insertClaim() {
        try {
            // the node_id condition never matches an existing claim of another node, the upsert fails on the duplicate _id instead
            collection.updateOne(
                    and(eq(FIELD_ID, BOOTSTRAP_ID), eq(FIELD_NODE_ID, nodeId.getNodeId())),
                    claimUpdate(),
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

    private boolean takeOverIfClaimantGone(Document claim) {
        final String claimant = claim.getString(FIELD_NODE_ID);
        final DataNodeDto claimantNode = nodeService.allActive().get(claimant);
        if (claimantNode != null && BOOTSTRAPPING_STATUSES.contains(claimantNode.getDataNodeStatus())) {
            LOG.info("Data node {} ({}) is bootstrapping the opensearch cluster, joining it", claimant, claim.getString(FIELD_HOSTNAME));
            return false;
        }

        final Bson claimExpired = expr(new Document("$lt", List.of(
                "$" + FIELD_CLAIMED_AT,
                new Document("$subtract", List.of("$$NOW", takeoverGracePeriod.toMillis()))
        )));

        if (refreshClaim(and(eq(FIELD_NODE_ID, claimant), claimExpired))) {
            LOG.warn("Data node {} claimed the opensearch cluster bootstrap but never started it, taking over the bootstrap", claimant);
            return true;
        }
        LOG.info("Waiting for data node {} ({}) to bootstrap the opensearch cluster", claimant, claim.getString(FIELD_HOSTNAME));
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
