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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.graylognode.MavenPackager;
import org.graylog.testing.graylognode.NodeContainerConfig;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ContainerizedGraylogBackend implements GraylogBackend, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackend.class);
    public static final String PASSWORD_SECRET = "M4lteserKreuzHerrStrack?-warZuKurzDeshalbMussdaNochWasdran";
    private static final Logger LOG = LoggerFactory.getLogger(ContainerizedGraylogBackend.class);
    public static final String PASSWORD_SECRET = "M4lteserKreuzHerrStrack?-warZuKurzDeshalbMussdaNochWasdranHasToBeAtLeastSixtyFourCharactersInLength";
    public static final String ROOT_PASSWORD_PLAINTEXT = "admin";
    public static final String ROOT_PASSWORD_SHA_2 = DigestUtils.sha256Hex(ROOT_PASSWORD_PLAINTEXT);

    private Network network;
    private SearchServerInstance searchServer;
    private MongoDBInstance mongodb;
    private MailServerContainer emailServerInstance;
    private NodeInstance node;

    private ContainerizedGraylogBackend() {}

    public synchronized static ContainerizedGraylogBackend createStarted(final SearchVersion version,
                                                                         final MongodbServer mongodbVersion,
                                                                         final int[] extraPorts,
                                                                         final List<URL> mongoDBFixtures,
                                                                         final PluginJarsProvider pluginJarsProvider,
                                                                         final MavenProjectDirProvider mavenProjectDirProvider,
                                                                         final List<String> enabledFeatureFlags,
                                                                         final boolean preImportLicense,
                                                                         final boolean withMailServerEnabled) {

        return new ContainerizedGraylogBackend().create(version, mongodbVersion, extraPorts, mongoDBFixtures, pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags, preImportLicense, withMailServerEnabled);
    }

    private ContainerizedGraylogBackend create(final SearchVersion version,
                                        final MongodbServer mongodbVersion,
                                        final int[] extraPorts,
                                        final List<URL> mongoDBFixtures,
                                        final PluginJarsProvider pluginJarsProvider,
                                        final MavenProjectDirProvider mavenProjectDirProvider,
                                        final List<String> enabledFeatureFlags,
                                        final boolean preImportLicense,
                                        final boolean withMailServerEnabled) {
        final var network = Network.newNetwork();
        final var builder = SearchServerInstanceProvider.getBuilderFor(version).orElseThrow(() -> new UnsupportedOperationException("Search version " + version + " not supported."));

        MongoDBInstance mongoDB = MongoDBInstance.createStartedWithUniqueName(network, Lifecycle.CLASS, mongodbVersion);
        if (withMailServerEnabled) {
            this.emailServerInstance = MailServerContainer.createStarted(network);
        }
        mongoDB.dropDatabase();
        mongoDB.importFixtures(mongoDBFixtures);

        MavenPackager.packageJarIfNecessary(mavenProjectDirProvider);

        SearchServerInstance searchServer = builder
                .network(network)
                .mongoDbUri(mongoDB.internalUri())
                .passwordSecret(PASSWORD_SECRET)
                .rootPasswordSha2(ROOT_PASSWORD_SHA_2)
                .featureFlags(enabledFeatureFlags)
                .build();

        if (preImportLicense) {
            createLicenses(mongoDB, "GRAYLOG_LICENSE_STRING", "GRAYLOG_SECURITY_LICENSE_STRING");
        }

        try {
            var nodeContainerConfig = new NodeContainerConfig(network, mongoDB.internalUri(), PASSWORD_SECRET, ROOT_PASSWORD_SHA_2, searchServer.internalUri(), searchServer.version(), extraPorts, pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags);
            NodeInstance node = NodeInstance.createStarted(nodeContainerConfig);
            this.network = network;
            this.searchServer = searchServer;
            this.mongodb = mongoDB;
            this.node = node;

            // ensure that all containers and networks will be removed after all tests finish
            // We can't close the resources in an afterAll callback, as the instances are cached and reused
            // so we need a solution that will be triggered only once after all test classes
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        } catch (Exception ex) {
            // if the graylog Node is not coming up (because OpenSearch hangs?) it fails here. So in this case, we also log the search server logs
            LOG.error("------------------------------ Search Server logs: --------------------------------------\n{}", searchServer.getLogs());
            throw ex;
        }
        return this;
    }

    private void createLicenses(final MongoDBInstance mongoDBInstance, final String... licenseStrs) {
        final List<String> licenses = Arrays.stream(licenseStrs).map(System::getenv).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if(!licenses.isEmpty()) {
            ServiceLoader<TestLicenseImporter> loader = ServiceLoader.load(TestLicenseImporter.class);
            loader.forEach(importer -> importer.importLicenses(mongoDBInstance, licenses));
        }
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

    public Optional<MailServerInstance> getEmailServerInstance() {
        return Optional.ofNullable(emailServerInstance);
    }

    @Override
    public String getSearchLogs() {
        return searchServer.getLogs();
    }

    @Override
    public void close() {
        node.close();
        searchServer.close();
        mongodb.close();

        if (emailServerInstance != null) {
            emailServerInstance.close();
        }

        network.close();
    }

    @Override
    public SearchServerInstance searchServerInstance() {
        return searchServer;
    }
}
