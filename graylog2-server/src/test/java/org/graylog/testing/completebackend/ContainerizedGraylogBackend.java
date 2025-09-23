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

import com.google.common.base.Stopwatch;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog.testing.completebackend.ContainerizedGraylogBackendServicesProvider.Services;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.graylognode.NodeContainerConfig;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * This backend implements {@link ExtensionContext.Store.CloseableResource} because then we can rely on junit's extension
 * stores to clean it up automatically without relying on VM shutdown hooks.
 */
public class ContainerizedGraylogBackend implements GraylogBackend, AutoCloseable, ExtensionContext.Store.CloseableResource {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerizedGraylogBackend.class);
    public static final String PASSWORD_SECRET = "M4lteserKreuzHerrStrack?-warZuKurzDeshalbMussdaNochWasdranHasToBeAtLeastSixtyFourCharactersInLength";
    public static final String ROOT_PASSWORD_PLAINTEXT = "admin";
    public static final String ROOT_PASSWORD_SHA_2 = DigestUtils.sha256Hex(ROOT_PASSWORD_PLAINTEXT);

    private final Services services;
    private final NodeInstance node;
    private final boolean stopServicesOnClose;

    private ContainerizedGraylogBackend(Services services,
                                        final List<URL> mongoDBFixtures,
                                        final PluginJarsProvider pluginJarsProvider,
                                        final MavenProjectDirProvider mavenProjectDirProvider,
                                        final List<String> enabledFeatureFlags,
                                        final boolean preImportLicense,
                                        Map<String, String> configParams) {
        this.services = services;
        // We don't want services for tests with custom flags or config params to be re-used, so we stop them
        // when the backend is closed.
        this.stopServicesOnClose = !enabledFeatureFlags.isEmpty() || !configParams.isEmpty();

        var mongoDB = services.getMongoDBInstance();
        LOG.info("Running backend with MongoDB version {} (instance: {})", mongoDB.version(), mongoDB.instanceId());
        mongoDB.importFixtures(mongoDBFixtures);

        if (preImportLicense) {
            createLicenses(mongoDB, "GRAYLOG_LICENSE_STRING", "GRAYLOG_SECURITY_LICENSE_STRING");
        }

        var searchServer = services.getSearchServerInstance();
        LOG.info("Running backend with SearchServer version {} (instance: {})", searchServer.version(), searchServer.instanceId());
        try {
            var nodeContainerConfig = new NodeContainerConfig(services.getNetwork(), MongoDBInstance.internalUri(), PASSWORD_SECRET, ROOT_PASSWORD_SHA_2, searchServer.internalUri(), searchServer.version(), pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags, configParams);
            this.node = NodeInstance.createStarted(nodeContainerConfig);
        } catch (Exception ex) {
            // if the graylog Node is not coming up (because OpenSearch hangs?) it fails here. So in this case, we also log the search server logs
            LOG.error("------------------------------ Search Server logs: --------------------------------------\n{}", searchServer.getLogs());
            throw ex;
        }
    }

    public synchronized static ContainerizedGraylogBackend createStarted(ContainerizedGraylogBackendServicesProvider servicesProvider,
                                                                         final SearchVersion version,
                                                                         final MongoDBVersion mongodbVersion,
                                                                         final List<URL> mongoDBFixtures,
                                                                         final PluginJarsProvider pluginJarsProvider,
                                                                         final MavenProjectDirProvider mavenProjectDirProvider,
                                                                         final List<String> enabledFeatureFlags,
                                                                         final boolean preImportLicense,
                                                                         Map<String, String> env,
                                                                         PluginJarsProvider datanodePluginJarsProvider) {

        final Stopwatch sw = Stopwatch.createStarted();
        LOG.debug("Creating Backend services {} {} flags <{}>", version, mongodbVersion, enabledFeatureFlags);
        final Services services = servicesProvider.getServices(version, mongodbVersion, enabledFeatureFlags, env, datanodePluginJarsProvider);
        LOG.debug(" creating backend services took " + sw.elapsed());

        final Stopwatch backendSw = Stopwatch.createStarted();
        final ContainerizedGraylogBackend backend = new ContainerizedGraylogBackend(services, mongoDBFixtures,
                pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags, preImportLicense, env);
        LOG.debug("Creating dockerized graylog server took {}", backendSw.elapsed());
        return backend;
    }

    private void createLicenses(final MongoDBInstance mongoDBInstance, final String... licenseStrs) {
        final List<String> licenses = Arrays.stream(licenseStrs)
                .map(System::getenv)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
        if (!licenses.isEmpty()) {
            ServiceLoader<TestLicenseImporter> loader = ServiceLoader.load(TestLicenseImporter.class);
            loader.forEach(importer -> importer.importLicenses(mongoDBInstance, licenses));
        }
    }

    @Override
    public void importElasticsearchFixture(String resourcePath, Class<?> testClass) {
        services.getSearchServerInstance().importFixtureResource(resourcePath, testClass);
    }

    @Override
    public void importMongoDBFixture(String resourcePath, Class<?> testClass) {
        services.getMongoDBInstance().importFixture(resourcePath, testClass);
    }

    @Override
    public void dropCollection(String collectionName) {
        services.getMongoDBInstance().dropCollection(collectionName);
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
        return services.getNetwork();
    }

    public Optional<MailServerInstance> getEmailServerInstance() {
        return Optional.ofNullable(services.getMailServerContainer());
    }

    @Override
    public Optional<WebhookServerInstance> getWebhookServerInstance() {
        return Optional.ofNullable(services.getWebhookServerContainer());
    }

    @Override
    public String getSearchLogs() {
        return services.getSearchServerInstance().getLogs();
    }

    @Override
    public void close() {
        node.close();
        if (stopServicesOnClose) {
            try {
                services.close();
            } catch (Exception e) {
                LOG.error("Error closing backend services", e);
                throw new RuntimeException(e);
            }
        } else {
            // Wipe SearchDB and MongoDB for next test run
            services.cleanUp();
        }
    }

    @Override
    public SearchServerInstance searchServerInstance() {
        return services.getSearchServerInstance();
    }
}
