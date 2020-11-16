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
package org.graylog.testing.completebackend;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GraylogBackend {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackend.class);
    private final ElasticsearchInstance es;
    private final MongoDBInstance mongodb;
    private final NodeInstance node;

    private static GraylogBackend instance;

    public static GraylogBackend createStarted(int[] extraPorts, ElasticsearchInstanceFactory elasticsearchInstanceFactory) {
        if (instance == null) {
            instance = createStartedBackend(extraPorts, elasticsearchInstanceFactory);
        } else {
            instance.fullReset();
            LOG.info("Reusing running backend");
        }

        return instance;
    }

    // Starting ES instance in parallel thread to save time.
    // MongoDB and the node have to be started in sequence however, because the the node might crash,
    // if a MongoDb instance isn't already present while it's starting up.
    private static GraylogBackend createStartedBackend(int[] extraPorts, ElasticsearchInstanceFactory elasticsearchInstanceFactory) {
        Network network = Network.newNetwork();

        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("build-es-container-for-api-it").build());

        Future<ElasticsearchInstance> esFuture = executor.submit(() -> elasticsearchInstanceFactory.create(network));

        MongoDBInstance mongoDB = MongoDBInstance.createStarted(network, MongoDBInstance.Lifecycle.CLASS);

        NodeInstance node = NodeInstance.createStarted(
                network,
                MongoDBInstance.internalUri(),
                ElasticsearchInstance.internalUri(),
                elasticsearchInstanceFactory.version(),
                extraPorts);

        try {
            return new GraylogBackend(esFuture.get(), mongoDB, node);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Container creation aborted", e);
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    private GraylogBackend(ElasticsearchInstance es, MongoDBInstance mongodb, NodeInstance node) {
        this.es = es;
        this.mongodb = mongodb;
        this.node = node;
    }

    public void purgeData() {
        mongodb.dropDatabase();
        es.cleanUp();
    }

    public void fullReset() {
        purgeData();
        node.restart();
    }

    public void importElasticsearchFixture(String resourcePath, Class<?> testClass) {
        es.importFixtureResource(resourcePath, testClass);
    }

    public String uri() {
        return node.uri();
    }

    public int apiPort() {
        return node.apiPort();
    }

    public void printServerLog() {
        node.printLog();
    }

    public int mappedPortFor(int originalPort) {
        return node.mappedPortFor(originalPort);
    }
}
