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

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;

public class MailServerInstance extends ExternalResource implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MailServerInstance.class);
    public static final int API_PORT = 8025;

    private final GenericContainer<?> container;

    public MailServerInstance(GenericContainer<?> container) {
        this.container = container;
    }

    public static MailServerInstance createStarted(Network network) {
        final GenericContainer<?> genericContainer = new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:v1.0.1"));
        genericContainer.withNetwork(network);
        genericContainer.withNetworkAliases("mailserver");
        genericContainer.addExposedPorts(1025, API_PORT);
        genericContainer.start();
        genericContainer.waitingFor(new HttpWaitStrategy().forPath("/api/v2/messages").forPort(API_PORT).withStartupTimeout(Duration.ofSeconds(10)));
        LOG.debug("Mailhog mailserver started");
        return new MailServerInstance(genericContainer);
    }

    public URI getEndpointURI() {
        return URI.create("http://" + container.getHost() + ":" + container.getMappedPort(API_PORT));
    }

    @Override
    public void close() {
        if (this.container != null) {
            this.container.close();
        }
    }
}
