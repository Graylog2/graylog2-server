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
import jakarta.annotation.Nonnull;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DockerMongodbConnectionResolver implements MongodbConnectionResolver {

    private final MongoDBContainer mongoDBContainer;
    private final Map<String, MongoClient> mongoClients = new ConcurrentHashMap<>();

    public DockerMongodbConnectionResolver(MongoDBContainer mongoDBContainer) {
        this.mongoDBContainer = mongoDBContainer;
    }

    /**
     * The nodeName is the internal representation of host:port inside the docker network. We'll be
     * accessing that from this integration test, from outside of the docker network, so we need to
     * map both host and port to something accessible from outside.
     */
    @Override
    public MongoClient resolve(String nodeName) {
        return mongoClients.computeIfAbsent(nodeName, this::createClient);
    }

    @Nonnull
    private MongoClient createClient(String nodeName) {
        final String[] hostPort = nodeName.split(":");
        final int port = Integer.parseInt(hostPort[1]);
        final Integer mappedPort = mongoDBContainer.getMappedPort(port);
        final String dockerHost = DockerClientFactory.instance().dockerHostIpAddress();
        return new MongoClient("mongodb://" + dockerHost + ":" + mappedPort + "/?directConnection=true");
    }

    public void close() {
        mongoClients.values().forEach(MongoClient::close);
    }
}
