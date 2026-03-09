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
import com.mongodb.MongoCommandException;
import com.mongodb.MongoSecurityException;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

  import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class MongodbClusterCommand {

    private static final Logger LOG = LoggerFactory.getLogger(MongodbClusterCommand.class);

    public static final String ADMIN_DATABASE_NAME = "admin";
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

        return knownHosts().map(hosts -> hosts.stream()
                        .parallel()
                        .collect(Collectors.toMap(host -> host, host -> runCommand(host, call))))
                .orElseGet(() -> {
                    final String host = connection.getClusterDescription().getServerDescriptions().getFirst().getAddress().toString();
                    return Collections.singletonMap(host, call.apply(host, connection));
                });
    }

    private Optional<List<String>> knownHosts() {
        final Document hello = connection.getDatabase(GRAYLOG_DATABASE_NAME).runCommand(new Document("hello", 1));
        if (hello.containsKey("hosts")) {
            // valid for replica set
            return Optional.of(hello.getList("hosts", String.class));
        } else {
            // this is the case for standalone instance
            return Optional.empty();
        }
    }

    private <T> T runCommand(String host, BiFunction<String, MongoClient, T> call) {
        try (final MongoClient client = mongodbConnectionResolver.resolve(host)) {
            return call.apply(host, client);
        } catch (MongoCommandException e) {
            // Error code 13 is "Unauthorized" in MongoDB
            if (e.getErrorCode() == 13) {
                LOG.error("Permission denied when executing command on MongoDB node '{}'. The MongoDB user may lack required privileges.", host, e);
                throw new MongodbPermissionException("Permission denied when executing command on MongoDB node '" + host + "'. Please ensure the MongoDB user has the required privileges.", e);
            }
            LOG.error("MongoDB command failed on node '{}'", host, e);
            throw e;
        } catch (MongoSecurityException e) {
            LOG.error("Security/authentication error when connecting to MongoDB node '{}'. Check MongoDB credentials and permissions.", host, e);
            throw new MongodbPermissionException("Security error when connecting to MongoDB node '" + host + "'. Please check MongoDB credentials and permissions.", e);
        }
    }
}
