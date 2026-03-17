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

import java.util.Collections;
import java.util.List;

public class StandaloneNodeMongodbNodes implements MongodbNodesService {

    private final MongoClient mongoConnection;

    @Inject
    public StandaloneNodeMongodbNodes(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection.connect();
    }

    @Override
    public List<MongodbNode> allNodes() {
        return Collections.singletonList(getNode());
    }

    private MongodbNode getNode() {
        Document serverStatus = mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME).runCommand(new Document("serverStatus", 1));

        // Extract basic information
        String version = serverStatus.getString("version");
        String host = serverStatus.getString("host");

        Document connections = serverStatus.get("connections", Document.class);
        final Integer availableConnections = connections.getInteger("available");
        final Integer currentConnections = connections.getInteger("current");
        final double connectionsPercent = 100.d * currentConnections / (availableConnections + currentConnections);

        // Get storage information
        double storageUsedPercent = MongodbNodeUtils.calculateStorageUsedPercent(mongoConnection);

        // Get slow query count
        ProfilingResult profilingResults = MongodbNodeUtils.getProfilingResults(mongoConnection);

        // For standalone nodes: role is "STANDALONE", status is 1 (primary equivalent), no replication lag
        return new MongodbNode("0", host, "STANDALONE", version, profilingResults.profilingLevel(), 0, profilingResults.slowQueryCount(), storageUsedPercent, availableConnections, currentConnections, connectionsPercent);
    }

    @Override
    public boolean available() {
        ClusterDescription clusterDescription = mongoConnection.getClusterDescription();
        ClusterType clusterType = clusterDescription.getType();
        return clusterType == ClusterType.STANDALONE;  // TODO: SHARDED and UNKNOWN?
    }
}
