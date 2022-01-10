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
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.Closeable;

/**
 * This rule starts an Elasticsearch instance and provides a configured {@link Client}.
 */
public interface SearchServerInstance extends Closeable {
    Client client();

    FixtureImporter fixtureImporter();

    GenericContainer<?> createContainer(String image, SearchVersion version, Network network);

    GenericContainer<?> buildContainer(String image, Network network);

    String internalUri();

    SearchVersion version();

    void importFixtureResource(String resourcePath, Class<?> testClass);

    String getHttpHostAddress();

    void cleanUp();

    @Override
    void close();
    protected void after() {
        cleanUp();
    }

    public void cleanUp() {
        client().cleanUp();
    }

    @Override
    public void close() {
        container.close();
        final List<SearchVersion> version = containersByVersion.keySet().stream().filter(k -> container == containersByVersion.get(k)).collect(Collectors.toList());
        version.forEach(containersByVersion::remove);
    }

    public static String internalUri() {
        return String.format(Locale.US, "http://%s:%d", NETWORK_ALIAS, ES_PORT);
    }

    public SearchVersion version() {
        return version;
    }

    public abstract SearchServer searchServer();

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


    public String getHttpHostAddress() {
        return this.container.getHost() + ":" + this.container.getMappedPort(9200);
    }

}
