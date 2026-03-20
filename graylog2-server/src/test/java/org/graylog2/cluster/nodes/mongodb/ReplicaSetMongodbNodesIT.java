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

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.graylog2.database.MongoConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;

class ReplicaSetMongodbNodesIT {

    private MongoDBContainer mongoDBContainer;
    private ReplicaSetMongodbNodes replicaSetNodes;

    @BeforeEach
    void setUp() {
        mongoDBContainer = new MongoDBContainer("mongo:" + MongoDBVersion.DEFAULT.version())
                .withReplicaSet();
        mongoDBContainer.start();
        final MongoConnection mongoConnection = createMongoConnection();
        replicaSetNodes = new ReplicaSetMongodbNodes(mongoConnection, new MongodbClusterCommand(mongoConnection, dockerConnectionResolver()));
    }

    /**
     * The nodeName is the internal representation of host:port inside the docker network. We'll be
     * accessing that from this integration test, from outside of the docker network, so we need to
     * map both host and port to something accessible from outside.
     */
    private MongodbConnectionResolver dockerConnectionResolver() {
        return nodeName -> {
            final String[] hostPort = nodeName.split(":");
            final int port = Integer.parseInt(hostPort[1]);
            final Integer mappedPort = mongoDBContainer.getMappedPort(port);
            final String dockerHost = DockerClientFactory.instance().dockerHostIpAddress();
            return new MongoClient("mongodb://" + dockerHost + ":" + mappedPort + "/?directConnection=true");
        };
    }

    @AfterEach
    void tearDown() {
        mongoDBContainer.stop();
    }


    @Test
    void testAllNodes() {
        final List<MongodbNode> nodes = replicaSetNodes.allNodes();
        Assertions.assertThat(nodes)
                .hasSize(1)
                .anySatisfy(node -> {
                    Assertions.assertThat(node.role()).isEqualTo("PRIMARY");
                    Assertions.assertThat(node.replicationLag()).isEqualTo(0); // primary can't have replication lag
                });

    }

    @Nonnull
    private MongoConnection createMongoConnection() {
        return new MongoConnection() {
            @Override
            public MongoClient connect() {
                return new MongoClient(mongoDBContainer.getReplicaSetUrl());
            }

            @Override
            public DB getDatabase() {
                throw new UnsupportedOperationException("Not supported here.");
            }

            @Override
            public MongoDatabase getMongoDatabase() {
                throw new UnsupportedOperationException("Not supported here.");
            }
        };
    }
}
