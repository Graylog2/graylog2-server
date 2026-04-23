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
package org.graylog2.events;

import com.github.joschi.jadconfig.util.Size;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class ClusterEventServiceShutdownTest {
    private MongoDBTestService mongodb;
    private MongoJackObjectMapperProvider objectMapperProvider;
    private Offset offset;

    @BeforeEach
    void setUp(MongoDBTestService mongodb, MongoJackObjectMapperProvider objectMapperProvider) {
        this.mongodb = mongodb;
        this.objectMapperProvider = objectMapperProvider;
        this.offset = new OffsetFromCurrentMongoDBTimeProvider(mongodb.mongoConnection()).get();
    }

    private ClusterEventService createClusterEventService(String nodeId, EventBus serverEventBus, ClusterEventBus clusterEventBus) {
        final var service = new ClusterEventService(objectMapperProvider, mongodb.mongoConnection(),
                new SimpleNodeId(nodeId),
                new RestrictedChainingClassLoader(new ChainingClassLoader(getClass().getClassLoader()),
                        SafeClasses.allGraylogInternal()),
                serverEventBus, clusterEventBus, offset, Size.megabytes(100));
        service.startAsync().awaitRunning();
        return service;
    }

    @Test
    void serviceProcessesEventAndShutsDown() throws Exception {
        final var serverEventBus = new EventBus();
        final var clusterEventBus = new ClusterEventBus();
        final var receivedLatch = new CountDownLatch(1);

        serverEventBus.register(new Object() {
            @Subscribe
            public void onEvent(DummyEvent event) {
                receivedLatch.countDown();
            }
        });

        final var consumerService = createClusterEventService("consumer", serverEventBus, clusterEventBus);

        final var producerEventBus = new EventBus();
        final var producerClusterBus = new ClusterEventBus();
        final var producerService = createClusterEventService("producer", producerEventBus, producerClusterBus);

        producerClusterBus.post(new DummyEvent("test-payload"));

        assertThat(receivedLatch.await(10, TimeUnit.SECONDS))
                .as("Event should be received by consumer")
                .isTrue();

        consumerService.stopAsync();
        consumerService.awaitTerminated(5, TimeUnit.SECONDS);
        producerService.stopAsync();
        producerService.awaitTerminated(5, TimeUnit.SECONDS);
    }
}
