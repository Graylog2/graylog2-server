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

import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

@ExtendWith(MongoDBExtension.class)
class MongodbNodesProviderIT {


    @Test
    void testValidResponse(MongoDBTestService mongoDBTestService) {
        final MongodbNodesProvider provider = new MongodbNodesProvider(mongoDBTestService.mongoConnection(), Set.of(dataReturningService()));
        final List<MongodbNode> results = provider.get();
        Assertions.assertThat(results)
                .hasSize(2)
                .extracting(MongodbNode::name)
                .contains("junit-first-node", "junit-second-node");
    }

    @Test
    void testFailure(MongoDBTestService mongoDBTestService) {
        final MongodbNodesProvider provider = new MongodbNodesProvider(mongoDBTestService.mongoConnection(), Set.of(failingService()));
        final List<MongodbNode> results = provider.get();
        Assertions.assertThat(results)
                .hasSize(1)
                .extracting(MongodbNode::name)
                .contains("localhost:" + mongoDBTestService.port());
    }

    private MongodbNodesService dataReturningService() {
        return new MongodbNodesService() {
            @Override
            public List<MongodbNode> allNodes() {
                return List.of(
                        new MongodbNode("0", "junit-first-node", "PRIMARY", "8.0.1", ProfilingLevel.OFF, 0L, 0L, 0d, 1, 1, 50d),
                        new MongodbNode("1", "junit-second-node", "PRIMARY", "8.0.1", ProfilingLevel.OFF, 0L, 0L, 0d, 1, 1, 50d)
                );
            }

            @Override
            public boolean available() {
                return true;
            }
        };
    }

    private MongodbNodesService failingService() {
        return new MongodbNodesService() {
            @Override
            public List<MongodbNode> allNodes() {
                throw new RuntimeException("Failed");
            }

            @Override
            public boolean available() {
                return true;
            }
        };
    }
}
