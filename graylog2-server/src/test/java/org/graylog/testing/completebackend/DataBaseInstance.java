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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.graylog2.database.MongoConnection;

import java.net.URL;
import java.util.List;

/**
 * Interface for database instance abstraction in tests.
 * Provides access to database operations for fixture imports, collection management, and database operations.
 */
public interface DataBaseInstance extends AutoCloseable {
    /**
     * Import a fixture from a resource path relative to the test class.
     *
     * @param resourceName the resource name or path
     * @param testClass the test class for resolving relative resources
     */
    void importFixture(String resourceName, Class<?> testClass);

    /**
     * Import multiple fixtures from a list of URLs.
     *
     * @param fixtureResources the list of fixture resource URLs
     */
    void importFixtures(List<URL> fixtureResources);

    /**
     * Get a MongoDB collection by name.
     *
     * @param name the collection name
     * @return the collection object
     */
    MongoCollection<Document> mongoCollection(String name);

    /**
     * Count documents in a collection.
     *
     * @param collection the collection name
     * @return the count of documents
     */
    long countDocuments(String collection);

    /**
     * Drop a collection by name.
     *
     * @param collectionName the collection name
     */
    void dropCollection(String collectionName);

    /**
     * Get the MongoDB database object.
     *
     * @return the database object
     */
    MongoDatabase mongoDatabase();

    /**
     * Drop the configured database.
     */
    void dropDatabase();

    /**
     * Get the MongoDB connection object.
     *
     * @return the connection object
     */
    MongoConnection mongoConnection();

    /**
     * Get the database instance ID.
     *
     * @return the instance ID
     */
    String instanceId();

    /**
     * Get the database version.
     *
     * @return the version object
     */
    Object version();

    /**
     * Get the internal URI for container-to-container communication.
     *
     * @return the internal URI
     */
    String internalUri();

    /**
     * Get the external URI for host-to-container communication.
     *
     * @return the external URI
     */
    String uri();

    /**
     * Close the database instance.
     */
    @Override
    void close();
}
