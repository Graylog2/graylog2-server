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
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ClusterType;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReplicaSetMongodbNodes implements MongodbNodesService {

    private final MongoClient mongoConnection;
    private final MongodbClusterCommand mongodbClusterCommand;

    @Inject
    public ReplicaSetMongodbNodes(MongoConnection mongoConnection, MongodbClusterCommand mongodbClusterCommand) {
        this.mongoConnection = mongoConnection.connect();
        this.mongodbClusterCommand = mongodbClusterCommand;
    }

    @Override
    public List<MongodbNode> allNodes() {
        Document replicaStatus = mongoConnection.getDatabase(MongodbClusterCommand.ADMIN_DATABASE_NAME).runCommand(new Document("replSetGetStatus", 1));

        final List<Document> members = replicaStatus.getList("members", Document.class);

        // Find primary's optime for replication lag calculation
        Document primaryMember = members.stream()
                .filter(m -> "PRIMARY".equals(m.getString("stateStr")))
                .findFirst()
                .orElse(null);


        return mongodbClusterCommand.runOnEachNode((host, connection) -> toMongodbNode(host, getMember(members, host), connection, primaryMember))
                .values()
                .stream()
                .toList();
    }

    /**
     * Lightweight replica-set membership: a single {@code replSetGetStatus} round-trip with no per-member fan-out.
     * Each returned {@link MongodbNode} carries only the fields available from the status document -- {@code id},
     * {@code name} (host), and {@code role} ({@code stateStr}) -- with the per-node stats left at their defaults.
     * Use this when you only need the roster and its roles; use {@link #allNodes()} when you need per-node
     * {@code serverStatus}/profiling/storage details (which cost one extra round-trip per member).
     *
     * @param timeout client-side operation timeout (CSOT). The read runs against a {@code withTimeout} database view
     *                so a stuck socket read fails fast rather than blocking indefinitely -- the Mongo driver's
     *                default socket timeout is infinite, so without this a black-holed connection never returns.
     */
    public List<MongodbNode> memberStates(Duration timeout) {
        final Document replicaStatus = mongoConnection.getDatabase(MongodbClusterCommand.ADMIN_DATABASE_NAME)
                .withTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .runCommand(new Document("replSetGetStatus", 1));
        return replicaStatus.getList("members", Document.class).stream()
                .map(m -> new MongodbNode(String.valueOf(m.get("_id", Integer.class)), m.getString("name"),
                        m.getString("stateStr")))
                .toList();
    }

    private Document getMember(List<Document> members, String host) {
        return members.stream().filter(m -> m.getString("name").equals(host)).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not find member " + host));
    }

    @Override
    public boolean available() {
        ClusterDescription clusterDescription = mongoConnection.getClusterDescription();
        ClusterType clusterType = clusterDescription.getType();
        return clusterType == ClusterType.REPLICA_SET;
    }

    private MongodbNode toMongodbNode(String hostname, Document member, MongoClient connection, Document primaryMember) {
        final Document serverStatus = connection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME).runCommand(new Document("serverStatus", 1));
        ProfilingResult profilingResult = MongodbNodeUtils.getProfilingResults(connection);

        Document connections = serverStatus.get("connections", Document.class);
        final Integer availableConnections = connections.getInteger("available");
        final Integer currentConnections = connections.getInteger("current");
        final double connectionsPercent = 100.d * currentConnections / (availableConnections + currentConnections);

        double storageUsedPercent = MongodbNodeUtils.calculateStorageUsedPercent(connection);

        int id = member.get("_id", Integer.class);
        String role = member.get("stateStr", String.class);

        // Replication lag - compare optime with primary
        long replicationLag = 0;
        if (primaryMember != null && !member.equals(primaryMember)) {
            if (member.containsKey("optimeDate") && primaryMember.containsKey("optimeDate")) {
                Date memberOptime = member.getDate("optimeDate");
                Date primaryOptime = primaryMember.getDate("optimeDate");
                if (memberOptime != null && primaryOptime != null) {
                    replicationLag = primaryOptime.getTime() - memberOptime.getTime();
                }
            }
        }
        return new MongodbNode(String.valueOf(id), hostname, role, serverStatus.getString("version"), profilingResult.profilingLevel(), replicationLag, profilingResult.slowQueryCount(), storageUsedPercent, availableConnections, currentConnections, connectionsPercent);
    }
}
