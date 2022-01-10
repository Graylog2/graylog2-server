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

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ContainerizedGraylogBackend implements GraylogBackend, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackend.class);
    private final Network network;
    private final SearchServerInstance searchServer;
    private final MongoDBInstance mongodb;
    private final NodeInstance node;

    // Starting ES instance in parallel thread to save time.
    // MongoDB and the node have to be started in sequence however, because the node might crash,
    // if a MongoDb instance isn't already present while it's starting up.
    public static ContainerizedGraylogBackend createStarted(int[] extraPorts, MongodbServer mongoVersion,
                                                            SearchServerInstanceFactory searchServerInstanceFactory, List<Path> pluginJars, Path mavenProjectDir,
                                                            List<URL> mongoDBFixtures) {
        Network network = Network.newNetwork();
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("build-es-container-for-api-it").build());
        Future<SearchServerInstance> esFuture = executor.submit(() -> searchServerInstanceFactory.create(network));
        MongoDBInstance mongoDB = MongoDBInstance.createStartedWithUniqueName(network, Lifecycle.CLASS, mongoVersion);
        mongoDB.dropDatabase();
        mongoDB.importFixtures(mongoDBFixtures);

        try {
            // Wait for ES before starting the Graylog node to avoid any race conditions
            SearchServerInstance esInstance = esFuture.get();
            NodeInstance node = NodeInstance.createStarted(
                    network,
                    MongoDBInstance.internalUri(),
                    esInstance.internalUri(),
                    searchServerInstanceFactory.getVersion(),
                    extraPorts,
                    pluginJars, mavenProjectDir);
            final ContainerizedGraylogBackend backend = new ContainerizedGraylogBackend(network, esInstance, mongoDB, node);

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

    private ContainerizedGraylogBackend(Network network, SearchServerInstance searchServer, MongoDBInstance mongodb, NodeInstance node) {
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

    @Override
    public void importElasticsearchFixture(String resourcePath, Class<?> testClass) {
        searchServer.importFixtureResource(resourcePath, testClass);
    }

    @Override
    public void importMongoDBFixture(String resourcePath, Class<?> testClass) {
        mongodb.importFixture(resourcePath, testClass);
    }

    @Override
    public String uri() {
        return node.uri();
    }

    @Override
    public int apiPort() {
        return node.apiPort();
    }

    @Override
    public int mappedPortFor(int originalPort) {
        return node.mappedPortFor(originalPort);
    }

    @Override
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

    @Override
    public SearchServerInstance searchServerInstance() {
        return searchServer;
    }
}
