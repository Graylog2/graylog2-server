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
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GraylogBackend implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackend.class);
    private final Network network;
    private final SearchServerInstance searchServer;
    private final MongoDBInstance mongodb;
    private final NodeInstance node;

    private static GraylogBackend instance;

    public static GraylogBackend createStarted(int[] extraPorts, MongodbServer mongoVersion,
                                               ElasticsearchInstanceFactory elasticsearchInstanceFactory, List<Path> pluginJars, Path mavenProjectDir,
                                               List<URL> mongoDBFixtures) {
        // if a cached version exists, shut it down first
        if (instance != null) {
            instance.close();
        }
        return doCreateStartedBackend(extraPorts, mongoVersion, elasticsearchInstanceFactory, pluginJars, mavenProjectDir,
                mongoDBFixtures);
    }

    /**
     * TODO: use or remove this method, currently is not used anywhere
     */
    public static GraylogBackend createStarted(int[] extraPorts,
                                               ElasticsearchInstanceFactory elasticsearchInstanceFactory, List<Path> pluginJars, Path mavenProjectDir,
                                               List<URL> mongoDBFixtures) {
        if (instance == null) {
            instance = doCreateStartedBackend(extraPorts, MongodbServer.DEFAULT_VERSION, elasticsearchInstanceFactory, pluginJars, mavenProjectDir,
                    mongoDBFixtures);
        } else {
            instance.fullReset(mongoDBFixtures);
            LOG.info("Reusing running backend");
        }
        return instance;
    }

    // Starting ES instance in parallel thread to save time.
    // MongoDB and the node have to be started in sequence however, because the the node might crash,
    // if a MongoDb instance isn't already present while it's starting up.
    private static GraylogBackend doCreateStartedBackend(int[] extraPorts, @Nonnull MongodbServer mongodbVersion,
                                                         ElasticsearchInstanceFactory elasticsearchInstanceFactory, List<Path> pluginJars, Path mavenProjectDir,
                                                         List<URL> mongoDBFixtures) {
        Network network = Network.newNetwork();
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("build-es-container-for-api-it").build());
        Future<SearchServerInstance> esFuture = executor.submit(() -> elasticsearchInstanceFactory.create(network));
        MongoDBInstance mongoDB = MongoDBInstance.createStartedWithUniqueName(network, Lifecycle.CLASS, mongodbVersion);
        mongoDB.dropDatabase();
        mongoDB.importFixtures(mongoDBFixtures);

        try {
            // Wait for ES before starting the Graylog node to avoid any race conditions
            SearchServerInstance esInstance = esFuture.get();
            NodeInstance node = NodeInstance.createStarted(
                    network,
                    MongoDBInstance.internalUri(),
                    SearchServerInstance.internalUri(),
                    elasticsearchInstanceFactory.getVersion(),
                    extraPorts,
                    pluginJars, mavenProjectDir);
            final GraylogBackend backend = new GraylogBackend(network, esInstance, mongoDB, node);

            // ensure that all containers and networks will be removed after all tests finish
            // We can't close the resources in an afterAll callback, as the instances are cached and reused
            // so we need a solution that will be triggered only once after all test classes
            Runtime.getRuntime().addShutdownHook(new Thread(backend::close));

            return backend;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Container creation aborted", e);
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    private GraylogBackend(Network network, SearchServerInstance searchServer, MongoDBInstance mongodb, NodeInstance node) {
        this.network = network;
        this.searchServer = searchServer;
        this.mongodb = mongodb;
        this.node = node;
    }

    public void purgeData() {
        mongodb.dropDatabase();
        searchServer.cleanUp();
    }

    public void fullReset(List<URL> mongoDBFixtures) {
        LOG.debug("Resetting backend.");
        purgeData();
        mongodb.importFixtures(mongoDBFixtures);
        node.restart();
    }

    public void importElasticsearchFixture(String resourcePath, Class<?> testClass) {
        searchServer.importFixtureResource(resourcePath, testClass);
    }

    public void importMongoDBFixture(String resourcePath, Class<?> testClass) {
        mongodb.importFixture(resourcePath, testClass);
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

    public Network network() {
        return this.network;
    }

    public MongoDBInstance mongoDB() {
        return mongodb;
    }

    @Override
    public void close() {
        node.close();
        mongodb.close();
        searchServer.close();
        network.close();
    }

    public SearchServerInstance elasticsearchInstance() {
        return searchServer;
    }
}
