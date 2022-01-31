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

import org.graylog.testing.containermatrix.SearchServer;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.Closeable;

public interface SearchServerInstance extends Closeable {
    Client client();

    SearchServer searchServer();

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
}
