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
        final List<Document> members = (List<Document>) replicaStatus.get("members");
        final List<MongodbNode> result = members.stream().map(this::toMongodbNode).toList();
        return new PaginatedList<>(result, members.size(), 1, members.size());
    }

    private MongodbNode toMongodbNode(Document member) {
        return new MongodbNode(member.get("name", String.class), member.get("stateStr", String.class));
    }
}
