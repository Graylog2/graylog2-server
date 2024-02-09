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
package org.graylog.datanode.testinfra;

import com.google.common.base.Suppliers;
import org.graylog.datanode.configuration.OpensearchArchitecture;
import org.graylog.testing.datanode.DatanodeDockerHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static org.graylog.datanode.testinfra.DatanodeContainerizedBackend.IMAGE_WORKING_DIR;
import static org.graylog.testing.completebackend.DefaultPluginJarsProvider.getProjectReposPath;
import static org.graylog.testing.completebackend.DefaultPluginJarsProvider.getProjectVersion;

public class DatanodeDevContainerBuilder implements org.graylog.testing.datanode.DatanodeDevContainerBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeDevContainerBuilder.class);
    private static final Supplier<ImageFromDockerfile> imageSupplier = Suppliers.memoize(DatanodeDevContainerBuilder::createImage);

    private String rootUsername = "admin";
    private String passwordSecret;
    private String rootPasswordSha2;
    private String mongoDbUri;
    private int restPort = 8999;
    private int openSearchHttpPort = 9200;
    private int openSearchTransportPort = 9300;
    private String nodeName = "node1";
    private Optional<DatanodeDockerHooks> customizer = Optional.empty();
    private Network network;

    protected static Path getPath() {
        return getProjectReposPath().resolve(Path.of("graylog2-server", "data-node", "target"));
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder mongoDbUri(final String mongoDbUri) {
        this.mongoDbUri = mongoDbUri;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder passwordSecret(final String passwordSecret) {
        this.passwordSecret = passwordSecret;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder rootPasswordSha2(final String rootPasswordSha2) {
        this.rootPasswordSha2 = rootPasswordSha2;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder rootUsername(final String rootUsername) {
        this.rootUsername = rootUsername;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder restPort(final int restPort) {
        this.restPort = restPort;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder openSearchHttpPort(final int openSearchHttpPort) {
        this.openSearchHttpPort = openSearchHttpPort;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder openSearchTransportPort(final int openSearchTransportPort) {
        this.openSearchTransportPort = openSearchTransportPort;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder nodeName(final String nodeName) {
        this.nodeName = nodeName;
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder customizer(final DatanodeDockerHooks hooks) {
        this.customizer = Optional.of(hooks);
        return this;
    }

    @Override
    public org.graylog.testing.datanode.DatanodeDevContainerBuilder network(final Network network) {
        this.network = network;
        return this;
    }

    public GenericContainer<?> build() {
        final Path graylog = getPath().resolve("graylog-datanode-" + getProjectVersion() + ".jar");
        if(!Files.exists(graylog)) {
            LOG.info("Searching for {} failed.", graylog.toAbsolutePath());
            LOG.info("Project repos path: {}, absolute path: {}", getProjectReposPath(), getProjectReposPath().toAbsolutePath());
            if(Files.exists(getPath())) {
                LOG.info("contents of base path {}:", getPath());
                try(var files = Files.list(getPath())) {
                    files.forEach(file -> LOG.info("{}", file.toString()));
                } catch (IOException ex) {
                    LOG.info("listing files failed with exception: {}", ex.getMessage());
                }
            } else {
                LOG.info("Base path {} does not exist.", getPath());
            }
            throw new RuntimeException("Failed to link graylog-datanode.jar to the datanode docker image, path " + graylog.toAbsolutePath() + " does not exist! Basepath, it was resolved from is: " + getProjectReposPath());
        }

        GenericContainer<?> container = new GenericContainer<>(imageSupplier.get())
                .withExposedPorts(restPort, openSearchHttpPort)
                .withNetwork(network)
                .withEnv("GRAYLOG_DATANODE_DATA_DIR", "data")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOCATION", IMAGE_WORKING_DIR)
                .withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "true")
                .withEnv("GRAYLOG_DATANODE_CONFIG_LOCATION", IMAGE_WORKING_DIR + "/config") // this is the datanode config dir for certs
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DATA_LOCATION", IMAGE_WORKING_DIR + "/opensearch/data")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOGS_LOCATION", IMAGE_WORKING_DIR + "/opensearch/logs")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_CONFIG_LOCATION", IMAGE_WORKING_DIR + "/opensearch/config")

                .withEnv("GRAYLOG_DATANODE_MONGODB_URI", mongoDbUri)
                .withEnv("GRAYLOG_DATANODE_NODE_NAME", nodeName)

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_HTTP_PORT", "" + openSearchHttpPort)
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_TRANSPORT_PORT", "" + openSearchTransportPort)
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", nodeName + ":" + openSearchTransportPort)

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_NETWORK_HOST", nodeName)
                .withEnv("GRAYLOG_DATANODE_CLUSTER_INITIAL_MANAGER_NODES", nodeName)

                .withEnv("GRAYLOG_DATANODE_ROOT_USERNAME", rootUsername)
                .withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", passwordSecret)
                .withEnv("GRAYLOG_DATANODE_ROOT_PASSWORD_SHA2", rootPasswordSha2)

                .withEnv("GRAYLOG_DATANODE_NODE_ID_FILE", "./node-id")
                .withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0:" + restPort)

                // disable disk threshold in tests, it causes problems in github builds where we don't have
                // enough free space
                .withEnv("opensearch.cluster.routing.allocation.disk.threshold_enabled", "false")

                .withNetworkAliases(nodeName)
                .waitingFor(new LogMessageWaitStrategy()
                        .withRegEx(".*Graylog DataNode datanode up and running.\n")
                        .withStartupTimeout(Duration.ofSeconds(60)));
        container.withFileSystemBind(graylog.toString(), IMAGE_WORKING_DIR + "/graylog-datanode.jar")
                .withFileSystemBind(getPath().resolve("lib").toString(), IMAGE_WORKING_DIR + "/lib/");

        customizer.ifPresent(c -> c.onContainer(container));
        return container;
    }

    private static String getOpensearchVersion() {
        try {
            final Properties props = new Properties();
            props.load(DatanodeContainerizedBackend.class.getResourceAsStream("/opensearch.properties"));
            return props.getProperty("opensearchVersion");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ImageFromDockerfile createImage() {
        final String opensearchTarArchive = "opensearch-" + getOpensearchVersion() + "-linux-" + OpensearchArchitecture.fromOperatingSystem();
        final Path downloadedOpensearch = getPath().resolve(Path.of("opensearch", opensearchTarArchive));

        if (!Files.exists(downloadedOpensearch)) {
            throw new RuntimeException("Failed to link opensearch distribution to the datanode docker image, path " + downloadedOpensearch.toAbsolutePath() + " does not exist!");
        }

        return new ImageFromDockerfile("local/graylog-datanode:latest", false)
                // the following command makes the opensearch tar.gz archive accessible in the docker build context, so it can
                // be later used by the ADD command
                .withFileFromPath(opensearchTarArchive, downloadedOpensearch)
                .withDockerfileFromBuilder(builder ->
                        builder.from("eclipse-temurin:17-jre-jammy")
                                .workDir(IMAGE_WORKING_DIR)
                                .run("mkdir -p opensearch/config")
                                .run("mkdir -p opensearch/data")
                                .run("mkdir -p opensearch/logs")
                                .run("mkdir -p config")

                                .add(opensearchTarArchive, "./" + opensearchTarArchive + "/") // this will automatically extract the tar
                                .run("touch datanode.conf") // create empty configuration file, required but all config comes via env props
                                .run("useradd opensearch")
                                .run("chown -R opensearch:opensearch " + IMAGE_WORKING_DIR)
                                .user("opensearch")
                                .expose(DatanodeContainerizedBackend.DATANODE_REST_PORT, DatanodeContainerizedBackend.DATANODE_OPENSEARCH_HTTP_PORT)
                                .entryPoint("java", "-jar", "graylog-datanode.jar", "datanode", "-f", "datanode.conf")
                                .build());
    }
}
