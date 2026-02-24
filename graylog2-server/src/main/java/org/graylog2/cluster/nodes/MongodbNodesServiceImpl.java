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
package org.graylog2.cluster.nodes;

import com.mongodb.MongoClient;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;

import java.util.Date;
import java.util.List;

public class MongodbNodesServiceImpl implements MongodbNodesService {

    private final MongoClient mongoConnection;

    @Inject
    public MongodbNodesServiceImpl(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection.connect();
    }

    @Override
    public PaginatedList<MongodbNode> searchPaginated(SearchQuery searchQuery, Bson bsonSort, int page, int perPage) {
        Document replicaStatus = mongoConnection.getDatabase("admin").runCommand(new Document("replSetGetStatus", 1));
        Document serverStatus = mongoConnection.getDatabase("admin").runCommand(new Document("serverStatus", 1));

        // TODO: can each replica member run different mongo version?
        final String version = serverStatus.getString("version");
        final List<Document> members = (List<Document>) replicaStatus.get("members");

        // Find primary's optime for replication lag calculation
        Document primaryMember = members.stream()
                .filter(m -> "PRIMARY".equals(m.getString("stateStr")))
                .findFirst()
                .orElse(null);

        double storageUsedPercent = calculateStorageUsedPercent();
        Long slowQueryCount = getSlowQueryCount();

        final List<MongodbNode> allNodes = members.stream()
                .map(member -> toMongodbNode(member, version, serverStatus, primaryMember, storageUsedPercent, slowQueryCount))
                .toList();

        final int totalCount = allNodes.size();

        // Apply pagination
        final int offset = (page - 1) * perPage;
        final List<MongodbNode> paginatedNodes = allNodes.stream()
                .skip(offset)
                .limit(perPage)
                .toList();

        return new PaginatedList<>(paginatedNodes, totalCount, page, perPage);
    }

    private MongodbNode toMongodbNode(Document member, String version, Document serverStatus, Document primaryMember,
                                      double storageUsedPercent, Long slowQueryCount) {
        String name = member.get("name", String.class);
        String role = member.get("stateStr", String.class);
        Integer status = member.getInteger("state");


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
        return new MongodbNode(name, role, version, status, replicationLag, slowQueryCount, storageUsedPercent);
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
