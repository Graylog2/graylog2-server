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
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MongodbNodesProvider implements Provider<List<MongodbNode>> {

    private static final Logger LOG = LoggerFactory.getLogger(MongodbNodesProvider.class);

    private final MongodbNodesService activeService;
    private final MongoClient mongoClient;

    @Inject
    public MongodbNodesProvider(MongoConnection mongoConnection, Set<MongodbNodesService> services) {
        this.mongoClient = mongoConnection.connect();
        activeService = services.stream()
                .filter(MongodbNodesService::available)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MongodbNodesService available"));
    }

    @Override
    public List<MongodbNode> get() {
        try {
            return activeService.allNodes();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Could not get MongodbNodes, returning fallback", e);
            } else {
                LOG.debug("Could not get MongodbNodes, returning fallback. Reason: {}", e.getMessage());
            }
            return getFallbackResponse();
        }
    }

    /**
     * This isn't touching any commands and isn't triggering any requests. It just returns what the current
     * mongodb client sees in the cluster description - usually at least one server address and its type.
     */
    @Nonnull
    private List<MongodbNode> getFallbackResponse() {
        final ClusterDescription clusterDescription = mongoClient.getClusterDescription();
        AtomicInteger id = new AtomicInteger(0);
        return clusterDescription.getServerDescriptions().stream()
                .map(serverDescription -> new MongodbNode("" + id.incrementAndGet(), serverDescription.getAddress().toString(), serverDescription.getType().toString()))
                .toList();
    }
}
