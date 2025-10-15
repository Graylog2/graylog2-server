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
import org.graylog.testing.graylognode.MavenPackager;
import org.graylog.testing.graylognode.NodeContainerConfig;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBFixtureImporter;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    private ContainerizedGraylogBackend(final ContainerizedGraylogBackendConfig config) {
        // We don't want services for tests with custom flags or config params to be re-used, so we stop them
        // when the backend is closed.
        this.stopServicesOnClose = !config.enabledFeatureFlags().isEmpty() || !config.env().isEmpty();

        // Create services. These are usually cached and re-used between tests.
        this.services = createServices(config);

        final var mongoDB = services.getMongoDBInstance();
        LOG.info("Running backend with MongoDB version {} (instance: {})", mongoDB.version(), mongoDB.instanceId());

        final var searchServer = services.getSearchServerInstance();
        LOG.info("Running backend with SearchServer version {} (instance: {})", searchServer.version(), searchServer.instanceId());

        if (config.importLicenses()) {
            createLicenses(mongoDB, "GRAYLOG_LICENSE_STRING", "GRAYLOG_SECURITY_LICENSE_STRING");
        }

        LOG.info("Creating server instance \"{}\"", config.serverProduct().name());
        final Stopwatch sw = Stopwatch.createStarted();
        try {
            final var nodeContainerConfig = new NodeContainerConfig(
                    services.getNetwork(),
                    MongoDBInstance.internalUri(),
                    PASSWORD_SECRET,
                    ROOT_PASSWORD_SHA_2,
                    searchServer.internalUri(),
                    searchServer.version(),
                    config.serverProduct().pluginJarsProvider(),
                    config.serverProduct().mavenProjectDirProvider(),
                    config.enabledFeatureFlags(),
                    config.env()
            );
            this.node = NodeInstance.createStarted(nodeContainerConfig);
        } catch (Exception ex) {
            // if the graylog Node is not coming up (because OpenSearch hangs?) it fails here. So in this case, we also log the search server logs
            LOG.error("------------------------------ Search Server logs: --------------------------------------\n{}", searchServer.getLogs());
            throw ex;
        }
        LOG.info("Creating the server instance took {}", sw.stop().elapsed());
    }

    private Services createServices(ContainerizedGraylogBackendConfig config) {
        LOG.debug("Creating Backend services: Search Server {}, MongoDB {}, feature-flags <{}>", config.searchServerVersion(), config.mongoDBVersion().version(), config.enabledFeatureFlags());
        final Stopwatch sw = Stopwatch.createStarted();
        final var services = config.serviceProvider().getServices(
                config.searchServerVersion(),
                config.mongoDBVersion(),
                config.enabledFeatureFlags(),
                config.env(),
                config.datanodeProduct().pluginJarsProvider()
        );
        LOG.debug("Creating Backend services took {}", sw.stop().elapsed());
        return services;
    }

    public synchronized static ContainerizedGraylogBackend createStarted(final ContainerizedGraylogBackendConfig config) {
        // Ensure that the server and Data Node are built before trying to start the containers.
        // TODO: The Maven packager only runs once per JVM. That means the first test that runs will build the JAR files.
        //       If that happens to be a test that uses the open server product, then only an open server will be built.
        //       Later Enterprise tests will then fail because the enterprise JAR files don't exist. This is only
        //       an issue when running multiple tests at once from an IDE.
        MavenPackager.packageJarIfNecessary(config.serverProduct().mavenProjectDirProvider());

        return new ContainerizedGraylogBackend(config);
    }

    private void createLicenses(final MongoDBInstance mongoDBInstance, final String... licenseStrs) {
        final List<String> licenses = Arrays.stream(licenseStrs)
                .map(System::getenv)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
        if (!licenses.isEmpty()) {
            ServiceLoader<TestLicenseImporter> loader = ServiceLoader.load(TestLicenseImporter.class);
            String clusterId = null;
            for (TestLicenseImporter importer : loader) {
                final Optional<String> licenseClusterId = importer.importLicenses(mongoDBInstance, licenses);
                if (licenseClusterId.isPresent() && clusterId == null) {
                    clusterId = licenseClusterId.get();
                }
            }
            if (clusterId == null) {
                LOG.warn("Unable to find the correct cluster id for the given licenses, tests might fail. Check your license strings.");
            } else {
                LOG.debug("Setting cluster id to {} from imported licenses", clusterId);
                new MongoDBFixtureImporter(Collections.emptyList()).importData(
                        mongoDBInstance.mongoConnection().getMongoDatabase(), """
                                  {
                                  "cluster_config": [
                                    {
                                      "_id": {
                                        "$oid": "6026a844d20e3a2e1ed57419"
                                      },
                                      "type": "org.graylog2.plugin.cluster.ClusterId",
                                      "payload": {
                                        "cluster_id": "%s"
                                      },
                                      "last_updated": {
                                        "$date": "2021-02-12T16:09:40.421Z"
                                      },
                                      "last_updated_by": "2d4cff7a-b9c4-440c-9c62-89ba1fb06211"
                                    }
                                  ]
                                }
                                """.formatted(clusterId));
            }
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
                LOG.info("Closing services");
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
