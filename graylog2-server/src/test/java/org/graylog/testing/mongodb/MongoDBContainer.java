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
package org.graylog.testing.mongodb;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Provides a MongoDB container.
 */
public class MongoDBContainer extends GenericContainer<MongoDBContainer> {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBContainer.class);
    public static final String DEFAULT_IMAGE = "mongo";

    // Run tests againtst the oldest supported MongoDB version
    public static final int MONGODB_PORT = 27017;
    public static final String NETWORK_ALIAS = "mongodb";

    // Attempts (100ms apart) to wait for the single-node replica set to elect a primary.
    private static final int AWAIT_REPLICA_SET_INIT_ATTEMPTS = 60;
    private static final int CONTAINER_EXIT_CODE_OK = 0;

    private boolean replicaSetEnabled;

    public static MongoDBContainer create(Network network) {
        return create(MongoDBVersion.DEFAULT, network);
    }

    public static MongoDBContainer create(MongoDBVersion version, Network network) {
        return new MongoDBContainer(DEFAULT_IMAGE + ":" + version.version(), network);
    }

    private MongoDBContainer(String dockerImageName, Network network) {
        super(requireNonNull(dockerImageName, "dockerImageName cannot be null"));
        withExposedPorts(MONGODB_PORT);
        withNetwork(requireNonNull(network, "network cannot be null"));
        withNetworkAliases(NETWORK_ALIAS);
        waitingFor(Wait.forListeningPort());

        // Workaround for running MongoDB 8.x (including the "latest" tag, which currently resolves to an 8.x release)
        // on Linux kernel version >= 6.19.
        // See: https://jira.mongodb.org/browse/SERVER-121912
        try {
            final var versionPart = DockerImageName.parse(dockerImageName).getVersionPart();
            if (versionPart.startsWith("8.") || versionPart.equals("latest")) {
                final var osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

                if (osName.contains("linux")) {
                    final var kernelVersion = System.getProperty("os.version", "0.0").split("\\.");
                    if (kernelVersion.length < 2) {
                        throw new IllegalStateException("Unexpected Linux kernel version: " + Arrays.toString(kernelVersion));
                    }
                    final int kernelMajorVersion = Objects.requireNonNullElse(Ints.tryParse(kernelVersion[0]), 0);
                    final int kernelMinorVersion = Objects.requireNonNullElse(Ints.tryParse(kernelVersion[1]), 0);

                    if (kernelMajorVersion >= 7 || (kernelMajorVersion == 6 && kernelMinorVersion >= 19)) {
                        LOG.info("Applying MongoDB 8.x workaround (running on Linux {})", System.getProperty("os.version"));
                        withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error applying Linux kernel version workaround for MongoDB 8.x", e);
        }
    }

    /**
     * Enables a single-node replica set. MongoDB does a 2-phase start with {@code --replSet}: it starts up,
     * then this container calls {@code rs.initiate()} and waits for the node to elect itself primary once
     * it reports itself started.
     */
    public MongoDBContainer withReplicaSet() {
        this.replicaSetEnabled = true;
        withCommand("--replSet", "docker-rs");
        waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1));
        return this;
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        if (replicaSetEnabled) {
            initReplicaSet();
        }
    }

    private void initReplicaSet() {
        try {
            LOG.debug("Initializing a single node replica set...");
            checkExitCode(execMongoEval("rs.initiate();"));
            checkExitCode(execMongoEval(waitForPrimaryCommand()));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to initialize MongoDB replica set", e);
        }
    }

    private static String waitForPrimaryCommand() {
        return String.format(Locale.ROOT,
                "var attempt = 0; while (db.runCommand({isMaster: 1}).ismaster == false) " +
                        "{ if (attempt > %d) { quit(1); } sleep(100); attempt++; }",
                AWAIT_REPLICA_SET_INIT_ATTEMPTS);
    }

    private ExecResult execMongoEval(String command) throws IOException, InterruptedException {
        return execInContainer("sh", "-c",
                "mongosh mongo --eval \"" + command + "\" || mongo --eval \"" + command + "\"");
    }

    private void checkExitCode(ExecResult result) {
        if (result.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            throw new IllegalStateException("MongoDB replica set command failed: " + result.getStdout());
        }
    }

    public String getConnectionString() {
        return String.format(Locale.ROOT, "mongodb://%s:%d", getHost(), getMappedPort(MONGODB_PORT));
    }

    public String getReplicaSetUrl() {
        return getConnectionString() + "/test";
    }

    public String infoString() {
        final InspectContainerResponse info = getContainerInfo();
        if (info != null) {
            return String.format(Locale.US, "%s%s/%s", info.getId(), info.getName(), info.getConfig().getImage());
        } else {
            LOG.warn("Could not get info from Docker container! getContainerInfo() returned null.");
            return "could not get info from container!";
        }
    }
}
