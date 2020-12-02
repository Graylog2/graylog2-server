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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class MongoDbConfigurationTest {
    @Test
    public void testGetMaximumMongoDBConnections() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_max_connections", "12345")), configuration).process();

        assertEquals(12345, configuration.getMaxConnections());
    }

    @Test
    public void testGetMaximumMongoDBConnectionsDefault() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();

        assertEquals(1000, configuration.getMaxConnections());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplier() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_threads_allowed_to_block_multiplier", "12345")), configuration).process();

        assertEquals(12345, configuration.getThreadsAllowedToBlockMultiplier());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplierDefault() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();

        assertEquals(5, configuration.getThreadsAllowedToBlockMultiplier());
    }

    @Test
    public void validateSucceedsIfUriIsMissing() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(Collections.emptyMap()), configuration).process();
        assertEquals("mongodb://localhost/graylog", configuration.getUri());
    }

    @Test(expected = ValidationException.class)
    public void validateFailsIfUriIsEmpty() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_uri", "")), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void validateFailsIfUriIsInvalid() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_uri", "Boom")), configuration).process();
    }

    @Test
    public void validateSucceedsIfUriIsValid() throws Exception {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final Map<String, String> properties = singletonMap(
                "mongodb_uri", "mongodb://example.com:1234,127.0.0.1:5678/TEST"
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertEquals("mongodb://example.com:1234,127.0.0.1:5678/TEST", configuration.getMongoClientURI().toString());
    }

    @Test
    public void validateSucceedsWithIPv6Address() throws Exception {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final Map<String, String> properties = singletonMap(
                "mongodb_uri", "mongodb://[2001:DB8::DEAD:BEEF:CAFE:BABE]:1234,127.0.0.1:5678/TEST"
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertEquals("mongodb://[2001:DB8::DEAD:BEEF:CAFE:BABE]:1234,127.0.0.1:5678/TEST", configuration.getMongoClientURI().toString());
    }
}
