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

import com.google.common.io.Resources;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Provides a MongoDB database service for tests.
 */
public class MongoDBTestService implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBTestService.class);

    private static final String DEFAULT_DATABASE_NAME = "graylog";

    private final MongoDBContainer container;
    private final MongoDBVersion version;
    private MongoConnectionImpl mongoConnection;

    /**
     * Create service instance with the default version and network and start it immediately.
     *
     * @return the service instance
     */
    public static MongoDBTestService createStarted(Network network) {
        return createStarted(MongoDBVersion.DEFAULT, network);
    }

    /**
     * Create service instance with the given version and network and start it immediately.
     *
     * @return the service instance
     */
    public static MongoDBTestService createStarted(MongoDBVersion version, Network network) {
        final var mongoDBTestService = new MongoDBTestService(MongoDBContainer.create(version, network), version);
        mongoDBTestService.start();
        return mongoDBTestService;
    }

    private MongoDBTestService(MongoDBContainer container, MongoDBVersion version) {
        this.container = requireNonNull(container, "container cannot be null");
        this.version = version;
    }

    /**
     * Starts the service and establishes a client connection. Will do nothing if service is already running.
     */
    private void start() {
        if (container.isRunning()) {
            return;
        }

        LOG.debug("Attempting to start container for image: {}", container.getDockerImageName());

        container.start();
        LOG.debug("Started container: {}", container.infoString());

        final MongoDbConfiguration mongoConfiguration = new MongoDbConfiguration();
        mongoConfiguration.setUri(uri());

        this.mongoConnection = new MongoConnectionImpl(mongoConfiguration);
        this.mongoConnection.connect();
        this.mongoConnection.getMongoDatabase().drop();
    }

    /**
     * Close (shutdown) the service.
     */
    @Override
    public void close() {
        container.close();
    }

    /**
     * Returns the established {@link MongoConnection} object.
     *
     * @return the connection object
     */
    public MongoConnection mongoConnection() {
        return requireNonNull(mongoConnection, "mongoConnection not initialized yet");
    }

    /**
     * Returns the database object for the current connection.
     *
     * @return the database object
     */
    public MongoDatabase mongoDatabase() {
        return mongoConnection().getMongoDatabase();
    }

    /**
     * Returns the collection object for the given collection name.
     *
     * @param name the collection name
     * @return the collection object
     */
    public MongoCollection<Document> mongoCollection(String name) {
        return mongoDatabase().getCollection(name);
    }

    /**
     * Drops the configured database.
     */
    public void dropDatabase() {
        if (container.isRunning()) {
            LOG.debug("Dropping database {}", mongoDatabase().getName());
            mongoDatabase().drop();
        }
    }

    /**
     * Returns the IP address of the database instance.
     *
     * @return the IP address
     */
    public String ipAddress() {
        return container.getContainerIpAddress();
    }

    /**
     * Returns the port of the database instance.
     *
     * @return the port
     */
    public int port() {
        return container.getFirstMappedPort();
    }

    /**
     * Returns the service instance ID.
     *
     * @return the instance ID
     */
    public String instanceId() {
        return container.getContainerId();
    }

    public String internalUri() {
        return uriWithHostAndPort(MongoDBContainer.NETWORK_ALIAS, MongoDBContainer.MONGODB_PORT);
    }

    public String uri() {
        return uriWithHostAndPort(ipAddress(), port());
    }

    private static String uriWithHostAndPort(String hostname, int port) {
        return String.format(Locale.US, "mongodb://%s:%d/%s", hostname, port, DEFAULT_DATABASE_NAME);
    }


    public void importFixtures(List<URL> fixtureResources) {
        if (!fixtureResources.isEmpty()) {
            new MongoDBFixtureImporter(fixtureResources).importResources(mongoDatabase());
        }
    }

    public void importFixture(String resourceName, Class<?> testClass) {
        if (!Paths.get(resourceName).isAbsolute()) {
            new MongoDBFixtureImporter(Arrays.asList(Resources.getResource(testClass, resourceName))).importResources(mongoDatabase());
        } else {
            new MongoDBFixtureImporter(Arrays.asList(Resources.getResource(resourceName))).importResources(mongoDatabase());
        }
    }

    public MongoDBVersion version() {
        return version;
    }
}
