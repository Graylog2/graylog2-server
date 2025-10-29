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

import com.github.joschi.jadconfig.util.Duration;
import jakarta.annotation.Nonnull;
import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.completebackend.DefaultMavenProjectDirProvider;
import org.graylog.testing.datanode.DatanodeDockerHooks;
import org.graylog.testing.graylognode.MavenPackager;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.security.JwtSecret;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.security.jwt.IndexerJwtAuthTokenProvider;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.List;

public class DatanodeContainerizedBackend {
    public static final String IMAGE_WORKING_DIR = "/usr/share/graylog/datanode";
    static public final String SIGNING_SECRET = ContainerizedGraylogBackend.PASSWORD_SECRET;

    public static final IndexerJwtAuthToken JWT_AUTH_TOKEN = createJwtAuthToken(SIGNING_SECRET, Duration.seconds(120), Duration.seconds(60), Duration.seconds(30));

    @Nonnull
    private static IndexerJwtAuthToken createJwtAuthToken(String signingSecret, Duration tokenExpirationDuration, Duration cachingDuration, Duration clockSkewTolerance) {
        return new IndexerJwtAuthTokenProvider(new JwtSecret(signingSecret), tokenExpirationDuration, cachingDuration, clockSkewTolerance, true, Clock.systemDefaultZone())
                .get();
    }

    public static final int DATANODE_REST_PORT = 8999;
    public static final int DATANODE_OPENSEARCH_HTTP_PORT = 9200;
    public static final int DATANODE_OPENSEARCH_TRANSPORT_PORT = 9300;

    private final Network network;
    private boolean shouldCloseNetwork = false;
    private final MongoDBTestService mongoDBTestService;
    private boolean shouldCloseMongodb = false;
    private final GenericContainer<?> datanodeContainer;
    private String nodeName;

    public DatanodeContainerizedBackend() {
        this(new DatanodeDockerHooksAdapter());
    }

    public DatanodeContainerizedBackend(DatanodeDockerHooks hooks) {
        this("node1", hooks);
    }

    public DatanodeContainerizedBackend(final String nodeName, DatanodeDockerHooks hooks) {

        this.network = Network.newNetwork();
        this.mongoDBTestService = MongoDBTestService.create(this.network);
        this.mongoDBTestService.start();

        // we have created these resources, we have to close them.
        this.shouldCloseNetwork = true;
        this.shouldCloseMongodb = true;

        this.datanodeContainer = createDatanodeContainer(
                nodeName,
                hooks);
    }

    public DatanodeContainerizedBackend(Network network, MongoDBTestService mongoDBTestService, String nodeName, DatanodeDockerHooks hooks) {
        this.network = network;
        this.mongoDBTestService = mongoDBTestService;
        this.datanodeContainer = createDatanodeContainer(
                nodeName,
                hooks);
    }

    private GenericContainer<?> createDatanodeContainer(String nodeName, DatanodeDockerHooks customizer) {
        this.nodeName = nodeName;
        MavenPackager.packageJarIfNecessary(new DefaultMavenProjectDirProvider());

        return new DatanodeDevContainerBuilder()
                .restPort(DATANODE_REST_PORT)
                .openSearchHttpPort(DATANODE_OPENSEARCH_HTTP_PORT)
                .openSearchTransportPort(DATANODE_OPENSEARCH_TRANSPORT_PORT)
                .mongoDbUri(mongoDBTestService.internalUri())
                .nodeName(nodeName)
                .network(network)
                .passwordSecret(ContainerizedGraylogBackend.PASSWORD_SECRET)
                .customizer(customizer)
                .build();
    }

    public DatanodeContainerizedBackend loadPlugins(List<Path> plugins) {
        plugins.forEach(hostPath -> {
            if (Files.exists(hostPath)) {
                final Path containerPath = Paths.get(IMAGE_WORKING_DIR, "plugin", hostPath.getFileName().toString());
                datanodeContainer.addFileSystemBind(hostPath.toString(), containerPath.toString(), BindMode.READ_ONLY);
            }
        });
        return this;
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
        return datanodeContainer.getMappedPort(DATANODE_OPENSEARCH_HTTP_PORT);
    }

    public Integer getOpensearchTransportPort() {
        return datanodeContainer.getMappedPort(DATANODE_OPENSEARCH_TRANSPORT_PORT);
    }

    public String getNodeName() {
        return nodeName;
    }
}
