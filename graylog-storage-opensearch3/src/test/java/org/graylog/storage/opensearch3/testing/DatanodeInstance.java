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
package org.graylog.storage.opensearch3.testing;

import org.graylog.testing.completebackend.DefaultPluginJarsProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.datanode.DatanodeDevContainerInstanceProvider;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatanodeInstance extends OpenSearchInstance {
    private final String mongoDBUri;
    private final String passwordSecret;
    private final PluginJarsProvider pluginJarsProvider;

    public DatanodeInstance(final boolean cachedInstance,
                            final SearchVersion version,
                            final String hostname,
                            final Network network,
                            final String mongoDBUri,
                            final String passwordSecret,
                            final String heapSize,
                            final List<String> featureFlags,
                            final Map<String, String> env,
                            final PluginJarsProvider pluginJarsProvider) {
        super(cachedInstance, version, hostname, network, heapSize, featureFlags, env, OpensearchSecurity.DISABLED);
        this.mongoDBUri = mongoDBUri;
        this.passwordSecret = passwordSecret;
        this.pluginJarsProvider = pluginJarsProvider;
    }

    @Override
    public DatanodeInstance init() {
        super.init();
        return this;
    }

    @Override
    protected String imageName() {
        return String.format(Locale.ROOT, "local/graylog-full-backend-test-datanode:%s", "latest");
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        final var builder = DatanodeDevContainerInstanceProvider.getBuilderFor(version())
                .orElseThrow(() -> new UnsupportedOperationException("Can not build container for search version " + version() + " - not supported."));

        final Path log4jhostPath = DefaultPluginJarsProvider.getProjectReposPath()
                .resolve(Path.of("graylog2-server", "data-node", "config", "log4j2.xml"));

        if (!Files.exists(log4jhostPath)) {
            throw new IllegalStateException("log4j2-server config file does not exist: " + log4jhostPath);
        }

        final String log4j2ContainerPath = "/usr/share/graylog/datanode/config/log4j2.xml";

        return builder
                .nodeName(hostname)
                .passwordSecret(passwordSecret)
                .network(network)
                .mongoDbUri(mongoDBUri)
                .restPort(8999)
                .openSearchHttpPort(9200)
                .openSearchTransportPort(9300)
                .env(getContainerEnv())
                .pluginJarsProvider(pluginJarsProvider)
                .customizer(container -> {
                    container.withFileSystemBind(
                            log4jhostPath.toString(),
                            log4j2ContainerPath,
                            BindMode.READ_ONLY
                    );
                    container.withEnv("JAVA_TOOL_OPTIONS", "-Dlog4j.configurationFile=file://" + log4j2ContainerPath);
                })
                .build();
    }
}
