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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class DatanodeContainerizedBackend {

    public static final int DATANODE_REST_PORT = 8999;
    public static final int DATANODE_OPENSEARCH_PORT = 9200;
    public static final String IMAGE_WORKING_DIR = "/usr/share/graylog/datanode";
    private final Network network;
    private final GenericContainer<?> mongodbContainer;
    private final GenericContainer<?> datanodeContainer;

    public DatanodeContainerizedBackend() {
        this(new DatanodeDockerHooksAdapter());
    }


    public DatanodeContainerizedBackend(DatanodeDockerHooks hooks) {
        this.network = Network.newNetwork();
        this.mongodbContainer = createMongodbContainer();
        this.datanodeContainer = createDatanodeContainer(
                "node1",
                hooks, createDockerImageFile(getOpensearchVersion()),
                getDatanodeVersion());
    }

    public DatanodeContainerizedBackend(Network network, GenericContainer<?> mongodbContainer, String nodeName, DatanodeDockerHooks hooks) {
        this.network = network;
        this.mongodbContainer = mongodbContainer;
        this.datanodeContainer = createDatanodeContainer(
                nodeName,
                hooks,
                createDockerImageFile(getOpensearchVersion()),
                getDatanodeVersion());
    }

    private GenericContainer<?> createDatanodeContainer(String nodeName, DatanodeDockerHooks customizer, ImageFromDockerfile image, String datanodeVersion) {
        GenericContainer<?> container = new GenericContainer<>(image)
                .withExposedPorts(DATANODE_REST_PORT, DATANODE_OPENSEARCH_PORT)
                .withNetwork(network)

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOCATION", IMAGE_WORKING_DIR)
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DATA_LOCATION", IMAGE_WORKING_DIR + "/data")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOGS_LOCATION", IMAGE_WORKING_DIR + "/logs")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_CONFIG_LOCATION", IMAGE_WORKING_DIR + "/config")
                .withEnv("GRAYLOG_DATANODE_CONFIG_LOCATION", IMAGE_WORKING_DIR + "/bin/config")

                .withEnv("GRAYLOG_DATANODE_MONGODB_URI", "mongodb://mongodb/graylog")
                .withEnv("GRAYLOG_DATANODE_NODE_NAME", nodeName)

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_HTTP_PORT", "" + DATANODE_OPENSEARCH_PORT)
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_TRANSPORT_PORT", "9300")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", "node1:9300")

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_NETWORK_HOST", nodeName)

                .withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", "admin")
                .withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", "admin")
                .withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", "this_is_not_used_but_required")

                .withEnv("GRAYLOG_DATANODE_NODE_ID_FILE", "./node-id")
                .withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0:" + DATANODE_REST_PORT)
                .withNetworkAliases(nodeName)
                .dependsOn(mongodbContainer)
                .waitingFor(new LogMessageWaitStrategy()
                        .withRegEx(".*Graylog DataNode datanode up and running.\n")
                        .withStartupTimeout(Duration.ofSeconds(60)));
        container.withFileSystemBind("target/graylog-datanode-" + datanodeVersion + ".jar", IMAGE_WORKING_DIR + "/graylog-datanode.jar")
                .withFileSystemBind("target/lib", IMAGE_WORKING_DIR + "/lib/");
        customizer.onContainer(container);
        return container;
    }

    private static ImageFromDockerfile createDockerImageFile(String opensearchVersion) {
        final String opensearchTarArchive = "opensearch-" + opensearchVersion + "-linux-x64.tar.gz";
        return new ImageFromDockerfile("local/graylog-datanode:latest", false)
                // the following command makes the opensearch tar.gz archive accessible in the docker build context, so it can
                // be later used by the ADD command
                .withFileFromPath(opensearchTarArchive, Path.of("target", "downloads", opensearchTarArchive))
                .withDockerfileFromBuilder(builder ->
                        builder.from("eclipse-temurin:17-jre-jammy")
                                .workDir(IMAGE_WORKING_DIR)
                                .run("mkdir -p config")
                                .run("mkdir -p config/opensearch")
                                .run("mkdir -p bin")
                                .run("mkdir -p bin/config")
                                .run("mkdir -p data")
                                .run("mkdir -p logs")
                                .add(opensearchTarArchive, ".") // this will automatically extract the tar
                                .run("touch datanode.conf") // create empty configuration file, required but all config comes via env props
                                .run("useradd opensearch")
                                .run("chown -R opensearch:opensearch " + IMAGE_WORKING_DIR)
                                .user("opensearch")
                                .expose(DATANODE_REST_PORT, DATANODE_OPENSEARCH_PORT)
                                .entryPoint("java", "-jar", "graylog-datanode.jar", "datanode", "-f", "datanode.conf")
                                .build());
    }

    private GenericContainer<?> createMongodbContainer() {
        return new GenericContainer<>("mongo:5.0")
                .withNetwork(network)
                .withNetworkAliases("mongodb")
                .withExposedPorts(27017);
    }


    public DatanodeContainerizedBackend start() {
        mongodbContainer.start();
        datanodeContainer.start();
        return this;
    }

    public void stop() {
        datanodeContainer.stop();
        mongodbContainer.stop();
        network.close();
    }

    public Network getNetwork() {
        return network;
    }

    public GenericContainer<?> getMongodbContainer() {
        return mongodbContainer;
    }

    public String getLogs() {
        return datanodeContainer.getLogs();
    }

    public Integer getDatanodeRestPort() {
        return datanodeContainer.getMappedPort(DATANODE_REST_PORT);
    }


    public Integer getOpensearchRestPort() {
        return datanodeContainer.getMappedPort(DATANODE_OPENSEARCH_PORT);
    }

    private String getDatanodeVersion() {
        try {
            final Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/version.properties"));
            return props.getProperty("project.version");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getOpensearchVersion() {
        try {
            final Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/opensearch.properties"));
            return props.getProperty("opensearchVersion");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
