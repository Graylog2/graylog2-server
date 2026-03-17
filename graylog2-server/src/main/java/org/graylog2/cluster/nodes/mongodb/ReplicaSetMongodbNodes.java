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

import java.util.Date;
import java.util.List;

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
