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
import com.mongodb.ReadConcern;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ClusterType;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;

import java.util.Date;
import java.util.List;

public class ReplicaSetMongodbNodes implements MongodbNodesProvider {

    private final MongoClient mongoConnection;

    @Inject
    public ReplicaSetMongodbNodes(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection.connect();

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

        double storageUsedPercent = calculateStorageUsedPercent();
        Long slowQueryCount = getSlowQueryCount();

        return members.stream()
                .parallel()
                .map(member -> toMongodbNode(member, primaryMember, storageUsedPercent, slowQueryCount))
                .toList();
    }

    @Override
    public boolean available() {
        ClusterDescription clusterDescription = mongoConnection.getClusterDescription();
        ClusterType clusterType = clusterDescription.getType();
        return clusterType == ClusterType.REPLICA_SET;
    }

    private MongodbNode toMongodbNode(Document member, Document primaryMember,
                                      double storageUsedPercent, Long slowQueryCount) {

        String name = member.get("name", String.class);

        String uri = "mongodb://" + name + "/?directConnection=true";
        try (com.mongodb.client.MongoClient nodeClient = MongoClients.create(uri)) {

            Document status = nodeClient
                    .getDatabase("admin")
                    .runCommand(new Document("serverStatus", 1));

            Document connections = status.get("connections", Document.class);
            final Integer availableConnections = connections.getInteger("available");
            final Integer currentConnections = connections.getInteger("current");
            final double connectionsPercent = 100.0d / availableConnections * currentConnections;

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
            return new MongodbNode(String.valueOf(id), name, role, status.getString("version"), replicationLag, slowQueryCount, storageUsedPercent, availableConnections, currentConnections, connectionsPercent);
        }
    }

    private double calculateStorageUsedPercent() {
        final Document dbStats = mongoConnection.getDatabase("admin").runCommand(new Document("dbStats", 1));
        return 100.0d * dbStats.getDouble("fsUsedSize") / dbStats.getDouble("fsTotalSize");
    }

    private Long getSlowQueryCount() {
        try {
            // Check if profiling is enabled and query system.profile
            Document profileStatus = mongoConnection.getDatabase("admin").runCommand(new Document("profile", -1));
            int profilingLevel = profileStatus.getInteger("was", 0);

            if (profilingLevel > 0) {
                // Count slow queries from the last 5 minutes
                long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
                Date cutoffTime = new Date(fiveMinutesAgo);

                Document query = new Document("ts", new Document("$gte", cutoffTime))
                        .append("millis", new Document("$gte", 100)); // Queries taking more than 100ms

                return mongoConnection.getDatabase("admin")
                        .getCollection("system.profile")
                        .countDocuments(query);
            }
        } catch (Exception e) {
            // Profiling may not be enabled or accessible
        }
        return null;
    }
}
