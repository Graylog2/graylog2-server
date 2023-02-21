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
        network = Network.newNetwork();

        mongodbContainer = new GenericContainer<>("mongo:5.0")
                .withNetwork(network)
                .withNetworkAliases("mongodb")
                .withExposedPorts(27017);

        final ImageFromDockerfile image = new ImageFromDockerfile("local/graylog-datanode:latest", false)
                .withDockerfileFromBuilder(builder ->
                        builder
                                .from("eclipse-temurin:17-jre-jammy")
                                .env("OPENSEARCH_VERSION", "2.4.1")
                                .workDir(IMAGE_WORKING_DIR)
                                // TODO: download clean version of opensearch or rely on the existing one from the build step? Mount or copy?
                                //.add("https://artifacts.opensearch.org/releases/bundle/opensearch/${OPENSEARCH_VERSION}/opensearch-${OPENSEARCH_VERSION}-linux-x64.tar.gz", "opensearch-dist/")
                                .run("mkdir -p config")
                                .run("mkdir -p data")
                                .run("mkdir -p logs")
                                .run("touch datanode.conf") // create empty configuration file, required but all config comes via env props
                                .run("useradd opensearch")
                                .run("chown -R opensearch:opensearch " + IMAGE_WORKING_DIR)
                                .user("opensearch")
                                .expose(DATANODE_REST_PORT, DATANODE_OPENSEARCH_PORT)
                                .entryPoint("java", "-jar", "datanode.jar", "datanode", "-f", "datanode.conf")
                                .build());

        datanodeContainer = new GenericContainer<>(image)
                .withExposedPorts(DATANODE_REST_PORT, DATANODE_OPENSEARCH_PORT)
                .withNetwork(network)

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOCATION", IMAGE_WORKING_DIR + "/opensearch-dist")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DATA_LOCATION", IMAGE_WORKING_DIR + "/data")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOGS_LOCATION", IMAGE_WORKING_DIR + "/logs")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_CONFIG_LOCATION", IMAGE_WORKING_DIR + "/config")

                .withEnv("GRAYLOG_DATANODE_MONGODB_URI", "mongodb://mongodb/graylog")
                .withEnv("GRAYLOG_DATANODE_NODE_NAME", "node1")

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_HTTP_PORT", "" + DATANODE_OPENSEARCH_PORT)
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_TRANSPORT_PORT", "9300")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", "127.0.0.1:9300")

                //datanode_transport_certificate=datanode-transport-certificates.p12
                //datanode_transport_certificate_password=password

                //datanode_http_certificate=datanode-http-certificates.p12
                //datanode_http_certificate_password=password

                .withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", "admin")
                .withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", "admin")
                .withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", "this_is_not_used_but_required")

                .withEnv("GRAYLOG_DATANODE_NODE_ID_FILE", "./node-id")
                .withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0:" + DATANODE_REST_PORT)

                .dependsOn(mongodbContainer)
                .waitingFor(new LogMessageWaitStrategy()
                        .withRegEx(".*Graylog DataNode datanode up and running.\n")
                        .withStartupTimeout(Duration.ofSeconds(60)));

        String datanodeVersion = getDatanodeVersion();
        String opensearchVersion = getOpensearchVersion();

        datanodeContainer
                .withFileSystemBind("target/datanode-" + datanodeVersion + ".jar", IMAGE_WORKING_DIR + "/datanode.jar")
                .withFileSystemBind("target/lib", IMAGE_WORKING_DIR + "/lib/")
                .withFileSystemBind("bin/opensearch-" + opensearchVersion, IMAGE_WORKING_DIR + "/opensearch-dist");
    }


    public void start() {
        mongodbContainer.start();
        datanodeContainer.start();
    }

    public void stop() {
        datanodeContainer.stop();
        mongodbContainer.stop();
        network.close();
    }

    public Integer getDatanodeRestPort() {
        return datanodeContainer.getMappedPort(DATANODE_REST_PORT);
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
