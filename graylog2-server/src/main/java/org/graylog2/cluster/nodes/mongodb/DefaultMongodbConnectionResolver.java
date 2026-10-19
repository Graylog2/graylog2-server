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

import com.google.common.net.HostAndPort;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.system.shutdown.GracefulShutdownHook;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultMongodbConnectionResolver implements MongodbConnectionResolver, GracefulShutdownHook {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMongodbConnectionResolver.class);

    // The per-node clients only run occasional cluster overview commands, so they don't need the configured pool size,
    // which is sized for the main connection.
    private static final int PER_NODE_MAX_POOL_SIZE = 5;

    private final MongoDbConfiguration configuration;
    private final Map<String, MongoClient> mongoClients = new ConcurrentHashMap<>();


    @Inject
    public DefaultMongodbConnectionResolver(MongoDbConfiguration configuration, GracefulShutdownService shutdownService) {
        // Don't parse the connection string here: for mongodb+srv:// that means DNS lookups
        this.configuration = configuration;
        shutdownService.register(DefaultMongodbConnectionResolver.this);
    }

    @Override
    public MongoClient resolve(String nodeName) {
        return mongoClients.computeIfAbsent(nodeName, k -> createClient(nodeName));
    }

    /**
     * Creates a client with a direct connection to a single node, reusing the credentials and options of the
     * configured connection string. They are taken from the connection string parsed by the MongoDB driver, which
     * already decoded them and, for mongodb+srv://, applied the implied TLS and the options of the DNS TXT record.
     */
    @Nonnull
    private MongoClient createClient(String nodeName) {
        LOG.info("Creating mongo client for {}", nodeName);
        final MongoClientURI uri = configuration.getMongoClientURI();
        final HostAndPort hostAndPort = HostAndPort.fromString(nodeName);
        final ServerAddress address = new ServerAddress(hostAndPort.getHost(), hostAndPort.getPortOrDefault(ServerAddress.defaultPort()));
        final MongoClientOptions options = MongoClientOptions.builder(uri.getOptions())
                .connectionsPerHost(PER_NODE_MAX_POOL_SIZE)
                .build();
        // This constructor connects directly to the given node, without discovering the other replica set members
        return new MongoClient(address, uri.getCredentials(), options);
    }

    @Override
    public void doGracefulShutdown() throws Exception {
        for (Map.Entry<String, MongoClient> entry : mongoClients.entrySet()) {
            final String nodeName = entry.getKey();
            final MongoClient client = entry.getValue();
            LOG.info("Closing mongo client for {}", nodeName);
            try {
                client.close();
            } catch (Exception e) {
                LOG.warn("Error while closing mongo client for {}", nodeName, e);
            }
        }
        mongoClients.clear();
    }
}
