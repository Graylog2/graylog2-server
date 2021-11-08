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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Provides a MongoDB container.
 */
public class MongoDBContainer extends GenericContainer<MongoDBContainer> {
    public static final String DEFAULT_IMAGE = "mongo";
    public static final String DEFAULT_VERSION = "3.6";
    public static final int MONGODB_PORT = 27017;
    public static final String NETWORK_ALIAS = "mongodb";

    public static MongoDBContainer create() {
        return create(Network.newNetwork());
    }

    public static MongoDBContainer create(String version) {
        return create(version, Network.newNetwork());
    }

    public static MongoDBContainer create(Network network) {
        return create(DEFAULT_VERSION, network);
    }

    public static MongoDBContainer create(String version, Network network) {
        return new MongoDBContainer(DEFAULT_IMAGE + ":" + version, network);
    }

    private MongoDBContainer(String dockerImageName, Network network) {
        super(requireNonNull(dockerImageName, "dockerImageName cannot be null"));
        withExposedPorts(MONGODB_PORT);
        withNetwork(requireNonNull(network, "network cannot be null"));
        withNetworkAliases(NETWORK_ALIAS);
        waitingFor(Wait.forListeningPort());
    }

    public String infoString() {
        final InspectContainerResponse info = getContainerInfo();
        return String.format(Locale.US, "%s%s/%s", info.getId(), info.getName(), info.getConfig().getImage());
    }
}
