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
import com.mongodb.MongoClientURI;
import jakarta.inject.Inject;
import org.graylog2.configuration.MongoDbConfiguration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class DefaultMongodbConnectionResolver implements MongodbConnectionResolver {

    private final MongoClientURI mongoClientURI;

    @Inject
    public DefaultMongodbConnectionResolver(MongoDbConfiguration configuration) {
        this.mongoClientURI = configuration.getMongoClientURI();
    }

    @Override
    public MongoClient resolve(String nodeName) {
        // Extract credentials from the original connection URI
        final String username = mongoClientURI.getUsername();
        final char[] password = mongoClientURI.getPassword();
        final String database = mongoClientURI.getDatabase();

        // Build connection string with credentials if they exist
        final String connectionString;
        if (username != null && password != null) {
            // URL encode username and password to handle special characters
            final String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            final String encodedPassword = URLEncoder.encode(new String(password), StandardCharsets.UTF_8);
            connectionString = String.format(Locale.ROOT, "mongodb://%s:%s@%s/%s?directConnection=true",
                    encodedUsername, encodedPassword, nodeName, database);
        } else {
            connectionString = String.format(Locale.ROOT, "mongodb://%s/?directConnection=true", nodeName);
        }

        return new MongoClient(new MongoClientURI(connectionString));
    }
}
