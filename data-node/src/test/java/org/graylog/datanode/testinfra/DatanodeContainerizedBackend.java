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
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.completebackend.DefaultMavenProjectDirProvider;
import org.graylog.testing.completebackend.DefaultPluginJarsProvider;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.datanode.DatanodeDockerHooks;
import org.graylog.testing.graylognode.MavenPackager;
import org.graylog.testing.graylognode.NodeContainerConfig;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

public class DatanodeContainerizedBackend {
    static public final String SIGNING_SECRET = ContainerizedGraylogBackend.PASSWORD_SECRET;

    public static final int DATANODE_REST_PORT = 8999;
    public static final int DATANODE_OPENSEARCH_PORT = 9200;
    public static final String IMAGE_WORKING_DIR = "/usr/share/graylog/datanode";
    private final Network network;
    private boolean shouldCloseNetwork = false;
    private final MongoDBTestService mongoDBTestService;
    private boolean shouldCloseMongodb = false;
    private final GenericContainer<?> datanodeContainer;

    private static final Supplier<ImageFromDockerfile> imageSupplier = Suppliers.memoize(DatanodeContainerizedBackend::createImage);

    public DatanodeContainerizedBackend() {
        this(new DatanodeDockerHooksAdapter());
    }

    public DatanodeContainerizedBackend(DatanodeDockerHooks hooks) {
        this("node1", hooks);
    }

    public DatanodeContainerizedBackend(final String nodeName, DatanodeDockerHooks hooks) {

        this.network = Network.newNetwork();
        this.mongoDBTestService = MongoDBTestService.create(MongodbServer.MONGO5, this.network);
        this.mongoDBTestService.start();

        // we have created these resources, we have to close them.
        this.shouldCloseNetwork = true;
        this.shouldCloseMongodb = true;

        this.datanodeContainer = createDatanodeContainer(
                nodeName,
                hooks,
                getDatanodeVersion());
    }

    public DatanodeContainerizedBackend(Network network, MongoDBTestService mongoDBTestService, String nodeName, DatanodeDockerHooks hooks) {
        this.network = network;
        this.mongoDBTestService = mongoDBTestService;
        this.datanodeContainer = createDatanodeContainer(
                nodeName,
                hooks,
                getDatanodeVersion());
    }

    private GenericContainer<?> createDatanodeContainer(String nodeName, DatanodeDockerHooks customizer, String datanodeVersion) {
        MavenPackager.packageJarIfNecessary(createConfig());

        GenericContainer<?> container = new GenericContainer<>(imageSupplier.get())
                .withExposedPorts(DATANODE_REST_PORT, DATANODE_OPENSEARCH_PORT)
                .withNetwork(network)

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOCATION", IMAGE_WORKING_DIR)
                .withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "true")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DATA_LOCATION", IMAGE_WORKING_DIR + "/datanode/data")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOGS_LOCATION", IMAGE_WORKING_DIR + "/datanode/logs")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_CONFIG_LOCATION", IMAGE_WORKING_DIR + "/datanode/config")

                .withEnv("GRAYLOG_DATANODE_MONGODB_URI", mongoDBTestService.internalUri())
                .withEnv("GRAYLOG_DATANODE_NODE_NAME", nodeName)

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_HTTP_PORT", "" + DATANODE_OPENSEARCH_PORT)
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_TRANSPORT_PORT", "9300")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_DISCOVERY_SEED_HOSTS", "node1:9300")

                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_NETWORK_HOST", nodeName)

                .withEnv("GRAYLOG_DATANODE_REST_API_USERNAME", "admin")
                .withEnv("GRAYLOG_DATANODE_REST_API_PASSWORD", "admin")
                .withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", SIGNING_SECRET)

                .withEnv("GRAYLOG_DATANODE_NODE_ID_FILE", "./node-id")
                .withEnv("GRAYLOG_DATANODE_HTTP_BIND_ADDRESS", "0.0.0.0:" + DATANODE_REST_PORT)

                // disable disk threshold in tests, it causes problems in github builds where we don't have
                // enough free space
                .withEnv("opensearch.cluster.routing.allocation.disk.threshold_enabled", "false")

                .withNetworkAliases(nodeName)
                .waitingFor(new LogMessageWaitStrategy()
                        .withRegEx(".*Graylog DataNode datanode up and running.\n")
                        .withStartupTimeout(Duration.ofSeconds(60)));
        container.withFileSystemBind("target/graylog-datanode-" + datanodeVersion + ".jar", IMAGE_WORKING_DIR + "/graylog-datanode.jar")
                .withFileSystemBind("target/lib", IMAGE_WORKING_DIR + "/lib/");
        customizer.onContainer(container);
        return container;
    }

    private NodeContainerConfig createConfig() {
        return new NodeContainerConfig(this.network, mongoDBTestService.internalUri(), SIGNING_SECRET, "rootPasswordSha2", null, null, new int[]{}, new DefaultPluginJarsProvider(),new DefaultMavenProjectDirProvider(), Collections.emptyList());
    }

    private static ImageFromDockerfile createImage() {
        final String opensearchTarArchive = "opensearch-" + getOpensearchVersion() + "-linux-" + OpensearchDistribution.archCode(System.getProperty("os.arch")) + ".tar.gz";
        final Path downloadedOpensearch = Path.of("target", "downloads", opensearchTarArchive);

        if(!Files.exists(downloadedOpensearch)) {
            throw new RuntimeException("Failed to link opensearch distribution to the datanode docker image, path" + downloadedOpensearch.toAbsolutePath() + " doesn't exist!");
        }

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

    public DatanodeContainerizedBackend start() {
        datanodeContainer.start();
        return this;
    }

    public void stop() {
        datanodeContainer.stop();
        if (shouldCloseMongodb) {
            mongoDBTestService.close();
        }
        if (shouldCloseNetwork) {
            network.close();
        }
    }

    public Network getNetwork() {
        return network;
    }

    public MongoDBTestService getMongoDb() {
        return mongoDBTestService;
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

    private static String getOpensearchVersion() {
        try {
            final Properties props = new Properties();
            props.load(DatanodeContainerizedBackend.class.getResourceAsStream("/opensearch.properties"));
            return props.getProperty("opensearchVersion");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
