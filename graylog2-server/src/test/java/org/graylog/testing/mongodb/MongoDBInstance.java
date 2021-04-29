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

import org.graylog2.database.MongoConnection;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This rule starts a MongoDB instance and provides a configured {@link org.graylog2.database.MongoConnection}.
 * <p>
 * Example usage:
 * <pre>{@code
 *   @Rule
 *   public final MongoDBInstance mongodb1 = MongoDBInstance.createForClass();
 *
 *   @Rule
 *   public final MongoDBInstance mongodb2 = MongoDBInstance.createForEachTest();
 * }</pre>
 */
public class MongoDBInstance extends ExternalResource implements AutoCloseable {
    public enum Lifecycle {
        METHOD, CLASS
    }

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBInstance.class);

    private static final String DEFAULT_INSTANCE_NAME = "default";
    private static final ConcurrentMap<String, MongoDBTestService> CACHED_SERVICE = new ConcurrentHashMap<>();

    private final Lifecycle lifecycle;
    private final MongoDBTestService service;

    private MongoDBFixtureImporter fixtureImporter;

    /**
     * Creates a new MongoDB instance that is shared for all test methods in a test class.
     *
     * @return the MongoDB instance
     */
    public static MongoDBInstance createForClass() {
        return createWithDefaults(Network.newNetwork(), Lifecycle.CLASS);
    }

    public static MongoDBInstance createWithDefaults(Network network, Lifecycle lifecycle) {
        return new MongoDBInstance(DEFAULT_INSTANCE_NAME, lifecycle, MongoDBContainer.DEFAULT_VERSION, network);
    }

    public static MongoDBInstance createStarted(Network network, Lifecycle lifecycle, List<URL> fixtureResources) {
        MongoDBInstance mongoDb = createWithDefaults(network, lifecycle);
        if (! fixtureResources.isEmpty()) {
            mongoDb.fixtureImporter = new MongoDBFixtureImporter(fixtureResources);
        }
        mongoDb.start();
        return mongoDb;
    }

    private MongoDBInstance(String instanceName, Lifecycle lifecycle, String version, Network network) {
        this.lifecycle = lifecycle;

        switch (lifecycle) {
            case CLASS:
                this.service = CACHED_SERVICE.computeIfAbsent(instanceName, k -> createContainer(version, network));
                break;
            case METHOD:
                this.service = createContainer(version, network);
                break;
            default:
                throw new IllegalArgumentException("Support for lifecycle " + lifecycle.toString() + " not implemented yet");
        }
    }

    private MongoDBTestService createContainer(String version, Network network) {
        return MongoDBTestService.create(version, network);
    }

    /**
     * Returns the established {@link MongoConnection} object.
     *
     * @return the connection object
     */
    public MongoConnection mongoConnection() {
        return service.mongoConnection();
    }

    /**
     * Drops the configured database.
     */
    public void dropDatabase() {
        service.dropDatabase();
    }

    public void start() {
        service.start();
        if (fixtureImporter != null) {
            fixtureImporter.importResources(service.mongoDatabase());
        }
    }

    @Override
    protected void before() {
        start();
    }

    public static String internalUri() {
        return MongoDBTestService.internalUri();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (description.getMethodName() != null) {
            final MongoDBFixtures fixtureFiles = description.getAnnotation(MongoDBFixtures.class);
            if (fixtureFiles != null) {
                LOG.debug("Loading fixtures {} for {}#{}()", fixtureFiles.value(), description.getTestClass().getCanonicalName(), description.getMethodName());
                this.fixtureImporter = new MongoDBFixtureImporter(fixtureFiles.value(), description.getTestClass());
            }
        }
        return super.apply(base, description);
    }

    @Override
    protected void after() {
        dropDatabase();
        switch (lifecycle) {
            case CLASS:
                break;
            case METHOD:
                close();
                break;
        }
    }

    /**
     * Stops the database instance.
     */
    @Override
    public void close() {
        try {
            service.close();
        } catch (Exception e) {
            LOG.error("Error closing service", e);
        }
    }
}
