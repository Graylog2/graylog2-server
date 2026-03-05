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
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class MongodbClusterCommand {

    public static final String GRAYLOG_DATABASE_NAME = "graylog";

    private final MongoClient connection;
    private final MongodbConnectionResolver mongodbConnectionResolver;

    @Inject
    public MongodbClusterCommand(MongoConnection mongoConnection, MongodbConnectionResolver mongodbConnectionResolver) {
        this.connection = mongoConnection.connect();
        this.mongodbConnectionResolver = mongodbConnectionResolver;
    }

    public Map<String, Document> runOnEachNode(Document command) {
        return runOnEachNode((host, connection) -> connection.getDatabase(GRAYLOG_DATABASE_NAME).runCommand(command));
    }

    public <T> Map<String, T> runOnEachNode(BiFunction<String, MongoClient, T> call) {
        final Document hello = connection.getDatabase(GRAYLOG_DATABASE_NAME).runCommand(new Document("hello", 1));
        final List<String> knownHosts = hello.getList("hosts", String.class);
        return knownHosts.stream()
                .parallel()
                .collect(Collectors.toMap(host -> host, host -> runCommand(host, call)));
    }

    private <T> T runCommand(String host, BiFunction<String, MongoClient, T> call) {
        try (final MongoClient client = mongodbConnectionResolver.resolve(host)) {
            return call.apply(host, client);
        }
    }
}
