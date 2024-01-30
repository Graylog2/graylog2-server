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
package org.graylog.testing.elasticsearch;

import com.google.common.io.Resources;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This rule starts a SearchServer instance and provides a configured {@link Client}.
 */
public abstract class TestableSearchServerInstance extends ExternalResource implements SearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(TestableSearchServerInstance.class);

    private static final Map<ContainerCacheKey, GenericContainer<?>> containersByVersion = new ConcurrentHashMap<>();

    protected static final int OPENSEARCH_PORT = 9200;

    private final SearchVersion version;
    protected final String heapSize;
    protected final Network network;
    protected final String hostname;
    protected GenericContainer<?> container;

    protected static volatile boolean isFirstContainerStart = true;

    @Override
    public abstract Client client();

    @Override
    public abstract FixtureImporter fixtureImporter();

    protected TestableSearchServerInstance(final SearchVersion version, final String hostname, final Network network, final String heapSize) {
        this.version = version;
        this.heapSize = heapSize;
        this.network = network;
        this.hostname = hostname;
    }

    protected abstract String imageName();

    public void createContainer() {
        this.container = createContainer(version, network, heapSize);
    }

    @Override
    public GenericContainer<?> createContainer(SearchVersion version, Network network, String heapSize) {
        final var image = imageName();
        final ContainerCacheKey cacheKey = new ContainerCacheKey(version, heapSize);
        if (!containersByVersion.containsKey(cacheKey)) {
            LOG.debug("Creating instance {}", image);
            GenericContainer<?> container = buildContainer(image, network);
            container.start();
            if (LOG.isDebugEnabled()) {
                container.followOutput(new Slf4jLogConsumer(LOG).withPrefix(image));
            }
            containersByVersion.put(cacheKey, container);
        } else {
            isFirstContainerStart = false;
        }
        LOG.debug("Using cached instance {}", image);
        return containersByVersion.get(cacheKey);
    }

    @Override
    protected void after() {
        cleanUp();
    }

    @Override
    public void cleanUp() {
        client().cleanUp();
    }

    @Override
    public void close() {
        LOG.debug("Closing instance {}", imageName());
        container.close();
        containersByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().equals(container))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()) // intermediate collect to avoid modifying the containersByVersion while we iterate over it
                .forEach(containersByVersion::remove);
    }

    @Override
    public String internalUri() {
        return String.format(Locale.US, "http://%s:%d", hostname, OPENSEARCH_PORT);
    }

    @Override
    public SearchVersion version() {
        return version;
    }

    @Override
    public void importFixtureResource(String resourcePath, Class<?> testClass) {
        boolean isFullResourcePath = Paths.get(resourcePath).getNameCount() > 1;

        @SuppressWarnings("UnstableApiUsage")
        final URL fixtureResource = isFullResourcePath
                ? Resources.getResource(resourcePath)
                : Resources.getResource(testClass, resourcePath);

        fixtureImporter().importResource(fixtureResource);

        // Make sure the data we just imported is visible
        client().refreshNode();
    }

    protected String getEsJavaOpts() {
        return StringUtils.f("-Xms%s -Xmx%s -Dlog4j2.formatMsgNoLookups=true", heapSize, heapSize);
    }

    @Override
    public String getHttpHostAddress() {
        return this.container.getHost() + ":" + this.container.getMappedPort(9200);
    }

    public TestableSearchServerInstance init() {
        createContainer();
        return this;
    }
}
