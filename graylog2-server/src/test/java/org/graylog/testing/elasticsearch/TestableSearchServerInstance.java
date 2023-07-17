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
import org.testcontainers.lifecycle.Startable;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

/**
 * This rule starts a Datanode instance and provides a configured {@link Client}.
 */
public abstract class TestableSearchServerInstance extends ExternalResource implements SearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(TestableSearchServerInstance.class);

    protected static final int OPENSEARCH_PORT = 9200;
    protected static final String NETWORK_ALIAS = "elasticsearch";

    private final SearchVersion version;
    protected final String heapSize;
    protected static Optional<GenericContainer<?>> container = Optional.empty();
    private String key = "";

    @Override
    public abstract Client client();

    @Override
    public abstract FixtureImporter fixtureImporter();

    protected TestableSearchServerInstance(String image, SearchVersion version, Network network, String heapSize) {
        this.version = version;
        this.heapSize = heapSize;
        createContainer(image, network);
    }

    private String createKey() {
        return version.toString() + "_" + heapSize;
    }

    @Override
    public void createContainer(String image, Network network) {
        var newKey = createKey();
        if (!key.equals(newKey)) {
            container.ifPresent(Startable::close);
            GenericContainer<?> c = buildContainer(image, network);
            c.start();
            if (LOG.isDebugEnabled()) {
                c.followOutput(new Slf4jLogConsumer(LOG));
            }
            container = Optional.of(c);
            key = newKey;
        }
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
        container.ifPresent(c -> {
            c.close();
            container = Optional.empty();
        });

    }

    @Override
    public String internalUri() {
        return String.format(Locale.US, "http://%s:%d", NETWORK_ALIAS, OPENSEARCH_PORT);
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
        return this.container.map(c -> c.getHost() + ":" + c.getMappedPort(9200)).orElse(null);
    }

}
