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
import org.apache.commons.lang3.StringUtils;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.graylognode.ExecutableNotFoundException;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ContainerizedGraylogBackend implements GraylogBackend, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackend.class);
    private Network network;
    private SearchServerInstance searchServer;
    private MongoDBInstance mongodb;
    private NodeInstance node;

    private ContainerizedGraylogBackend() {
    }

    public static ContainerizedGraylogBackend createStarted(SearchVersion esVersion, MongodbServer mongodbVersion,
                                                            int[] extraPorts, List<URL> mongoDBFixtures,
                                                            PluginJarsProvider pluginJarsProvider, MavenProjectDirProvider mavenProjectDirProvider,
                                                            List<String> enabledFeatureFlags, boolean preImportLicense) {

        final ContainerizedGraylogBackend backend = new ContainerizedGraylogBackend();
        backend.create(esVersion, mongodbVersion, extraPorts, mongoDBFixtures, pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags, preImportLicense);
        return backend;
    }

    private void create(SearchVersion esVersion, MongodbServer mongodbVersion,
                        int[] extraPorts, List<URL> mongoDBFixtures,
                        PluginJarsProvider pluginJarsProvider, MavenProjectDirProvider mavenProjectDirProvider,
                        List<String> enabledFeatureFlags, boolean preImportLicense) {

        final SearchServerInstanceFactory searchServerInstanceFactory = new SearchServerInstanceFactoryByVersion(esVersion);
        Network network = Network.newNetwork();
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("build-es-container-for-api-it").build());
        Future<SearchServerInstance> esFuture = executor.submit(() -> searchServerInstanceFactory.create(network));
        MongoDBInstance mongoDB = MongoDBInstance.createStartedWithUniqueName(network, Lifecycle.CLASS, mongodbVersion);
        mongoDB.dropDatabase();
        mongoDB.importFixtures(mongoDBFixtures);

        if(preImportLicense) {
            createLicenses(mongoDB, "GRAYLOG_LICENSE_STRING", "GRAYLOG_SECURITY_LICENSE_STRING");
        }

        try {
            // Wait for ES before starting the Graylog node to avoid any race conditions
            SearchServerInstance esInstance = esFuture.get();
            NodeInstance node = NodeInstance.createStarted(
                    network,
                    MongoDBInstance.internalUri(),
                    esInstance.internalUri(),
                    esInstance.version(),
                    extraPorts,
                    pluginJarsProvider, mavenProjectDirProvider,
                    enabledFeatureFlags);
            this.network = network;
            this.searchServer = esInstance;
            this.mongodb = mongoDB;
            this.node = node;

            // ensure that all containers and networks will be removed after all tests finish
            // We can't close the resources in an afterAll callback, as the instances are cached and reused
            // so we need a solution that will be triggered only once after all test classes
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        } catch (InterruptedException | ExecutionException | ExecutableNotFoundException e) {
            LOG.error("Container creation aborted", e);
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    private void createLicenses(MongoDBInstance mongoDBInstance, String... licenseStrs) {
        try {
            final List<String> licenses = Arrays.stream(licenseStrs).map(System::getenv).filter(p -> StringUtils.isNotBlank(p)).collect(Collectors.toList());
            if(!licenses.isEmpty()) {
                Class myClass = Class.forName("testing.completebackend.EnterpriseITUtils");
                Method method = myClass.getDeclaredMethod("importLicense", MongoDBInstance.class, String.class);
                for (String license : licenses) {
                    method.invoke(null, mongoDBInstance, license);
                }
            }
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException ex) {
            LOG.error("Could not import license to Mongo: ", ex);
        }
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
    public String getLogs() {
        return node.getLogs();
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
