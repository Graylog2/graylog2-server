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
package org.graylog.testing.graylognode;

import com.google.common.base.Stopwatch;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.Closeable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.graylog.testing.graylognode.NodeContainerConfig.API_PORT;

public class NodeInstance implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);

    private final GenericContainer<?> container;

    public static NodeInstance createStarted(Network network, String mongoDbUri, String elasticsearchUri, SearchVersion elasticsearchVersion, int[] extraPorts,
                                             PluginJarsProvider pluginJarsProvider, MavenProjectDirProvider mavenProjectDirProvider,
                                             List<String> enabledFeatureFlags) {
        NodeContainerConfig config = new NodeContainerConfig(network, mongoDbUri, elasticsearchUri, elasticsearchVersion, extraPorts, pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags);
        GenericContainer<?> container = NodeContainerFactory.buildContainer(config);
        return new NodeInstance(container);
    }

    public NodeInstance(GenericContainer<?> container) {
        this.container = container;
    }

    public void restart() {
        Stopwatch sw = Stopwatch.createStarted();
        container.stop();
        container.start();
        sw.stop();
        LOG.info("Restarted node container in " + sw.elapsed(TimeUnit.SECONDS));
    }

    public String uri() {
        return String.format(Locale.US, "http://%s", container.getContainerIpAddress());
    }

    public int apiPort() {
        return mappedPortFor(API_PORT);
    }

    public int mappedPortFor(int originalPort) {
        return container.getMappedPort(originalPort);
    }

    public String getLogs() {
        return container.getLogs();
    }

    @Override
    public void close() {
        container.close();
    }
}
