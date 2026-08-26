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
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;

class StandaloneNodeMongodbNodesIT {

    private MongoDBContainer mongoDBContainer;
    private StandaloneNodeMongodbNodes standaloneNodes;

    @BeforeEach
    void setUp() {
        mongoDBContainer = new MongoDBContainer("mongo:" + MongoDBVersion.DEFAULT.version());
        mongoDBContainer.start();
        standaloneNodes = new StandaloneNodeMongodbNodes(createMongoConnection());
    }

    @AfterEach
    void tearDown() {
        mongoDBContainer.stop();
    }


    @Test
    void testAllNodes() {
        final List<MongodbNode> nodes = standaloneNodes.allNodes();
        Assertions.assertThat(nodes)
                .hasSize(1)
                .anySatisfy(node -> {
                    Assertions.assertThat(node.role()).isEqualTo("STANDALONE");
                    Assertions.assertThat(node.replicationLag()).isEqualTo(0); // standalone nodes have no replication lag
                });

    }

    @Nonnull
    private MongoConnection createMongoConnection() {
        return new MongoConnection() {
            @Override
            public MongoClient connect() {
                return new MongoClient(mongoDBContainer.getConnectionString());
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
