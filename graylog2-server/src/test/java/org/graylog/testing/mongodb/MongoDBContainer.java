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


        // Workaround for running MongoDB 8.x on Linux kernel version >= 6.19
        // See: https://jira.mongodb.org/browse/SERVER-121912
        try {
            if (DockerImageName.parse(dockerImageName).getVersionPart().startsWith("8.")) {
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
