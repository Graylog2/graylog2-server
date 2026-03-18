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

import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.graylog2.configuration.MongoDbConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultMongodbConnectionResolver implements MongodbConnectionResolver {

    private final MongoClientURI mongoClientURI;

    @Inject
    public DefaultMongodbConnectionResolver(MongoDbConfiguration configuration) {
        this.mongoClientURI = configuration.getMongoClientURI();
    }

    @Override
    public MongoClient resolve(String nodeName) {
        return new MongoClient(new MongoClientURI(buildConnectionString(nodeName)));
    }

    private String buildConnectionString(String nodeName) {
        // Extract credentials from the original connection URI
        final String username = mongoClientURI.getUsername();
        final char[] password = mongoClientURI.getPassword();
        final String database = mongoClientURI.getDatabase();

        // Parse host and port from the nodeName (supports both IPv4 and IPv6)
        final HostAndPort hostAndPort = HostAndPort.fromString(nodeName);

        try {
            URIBuilder builder = new URIBuilder();
            builder.setScheme("mongodb");
            builder.setHost(hostAndPort.getHost());
            builder.setPort(hostAndPort.getPort());
            if (username != null && password != null) {
                builder.setUserInfo(String.format("%s:%s", username, new String(password)));
            }
            builder.setPath(database);
            final Map<String, String> configParams = new LinkedHashMap<>(configParamsFromQuery());
            configParams.put("directConnection", "true");
            configParams.forEach(builder::addParameter);

            return builder.build().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private Map<String, String> configParamsFromQuery() {
        final URI uri = URI.create(mongoClientURI.getURI());
        return Optional.ofNullable(uri.getQuery())
                .map(query -> Splitter.on('&')
                        .withKeyValueSeparator('=')
                        .split(query))
                .orElseGet(Collections::emptyMap);
    }
}
