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
    private final MongodbConnectionResolver connectionResolver;

    @Inject
    public ReplicaSetMongodbNodes(MongoConnection mongoConnection, MongodbConnectionResolver connectionResolver) {
        this.mongoConnection = mongoConnection.connect();
        this.connectionResolver = connectionResolver;

        Document command = new Document("profile", 1)
                .append("slowms", 100);

        this.mongoConnection.getDatabase("admin").runCommand(command);

    }

    @Override
    public List<MongodbNode> allNodes() {
        Document replicaStatus = mongoConnection.getDatabase("admin").runCommand(new Document("replSetGetStatus", 1));
        //Document serverStatus = mongoConnection.getDatabase("admin").runCommand(new Document("serverStatus", 1));

        // TODO: can each replica member run different mongo version?
        //final String version = serverStatus.getString("version");
        final List<Document> members = (List<Document>) replicaStatus.get("members");

        // Find primary's optime for replication lag calculation
        Document primaryMember = members.stream()
                .filter(m -> "PRIMARY".equals(m.getString("stateStr")))
                .findFirst()
                .orElse(null);

        return members.stream()
                .parallel()
                .map(member -> toMongodbNode(member, primaryMember))
                .toList();
    }

    @Override
    public boolean available() {
        ClusterDescription clusterDescription = mongoConnection.getClusterDescription();
        ClusterType clusterType = clusterDescription.getType();
        return clusterType == ClusterType.REPLICA_SET;
    }

    private MongodbNode toMongodbNode(Document member, Document primaryMember) {

        String name = member.get("name", String.class);

        try (MongoClient nodeClient = connectionResolver.resolve(name)) {

            Document serverStatus = nodeClient
                    .getDatabase("admin")
                    .runCommand(new Document("serverStatus", 1));

            ProfilingResult profilingResult = MongodbNodeUtils.getProfilingResults(nodeClient);

            Document connections = serverStatus.get("connections", Document.class);
            final Integer availableConnections = connections.getInteger("available");
            final Integer currentConnections = connections.getInteger("current");
            final double connectionsPercent = 100.0d / availableConnections * currentConnections;

            double storageUsedPercent = MongodbNodeUtils.calculateStorageUsedPercent(nodeClient);

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
            return new MongodbNode(String.valueOf(id), name, role, serverStatus.getString("version"), profilingResult.profilingLevel(), replicationLag, profilingResult.slowQueryCount(), storageUsedPercent, availableConnections, currentConnections, connectionsPercent);
        }
    }
}
