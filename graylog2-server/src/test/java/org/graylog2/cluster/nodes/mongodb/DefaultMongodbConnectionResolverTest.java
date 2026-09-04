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
package org.graylog2.cluster.nodes.mongodb;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ServerDescription;
import org.assertj.core.api.Assertions;
import org.graylog2.configuration.MongoDbConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultMongodbConnectionResolverTest {

    TestShutdownService shutdownService = new TestShutdownService();

    @AfterEach
    void tearDown() {
        shutdownService.shutDown();
    }

    @Test
    void testResolveWithoutAuthentication() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://localhost:27017/graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        assertThat(resolver.resolve("host1:27017"))
                .isNotNull()
                .satisfies(assertServer("host1:27017"))
                .extracting(MongoClient::getCredential)
                .isNull();
    }

    @Test
    void testResolveWithAuthentication() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://testuser:testpass@localhost:27017/graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        assertThat(resolver.resolve("host1:27017"))
                .isNotNull()
                .satisfies(assertServer("host1:27017"))
                .satisfies(assertCredentials("graylog", "testuser", "testpass"));

    }

    @Test
    void testResolveWithSpecialCharactersInPassword() throws Exception {
        // Password contains special characters that need URL encoding: @, +, =
        MongoDbConfiguration config = createConfiguration("mongodb://user:p%40ss%2Bw%3Drd@localhost:27017/graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        assertThat(resolver.resolve("host1:27017"))
                .isNotNull()
                .satisfies(assertServer("host1:27017"))
                .satisfies(assertCredentials("graylog", "user", "p@ss+w=rd"));
    }

    @Test
    void testResolveWithSpecialCharactersInUsername() throws Exception {
        // Username contains special characters that need URL encoding
        MongoDbConfiguration config = createConfiguration("mongodb://user%40domain:password@localhost:27017/graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        assertThat(resolver.resolve("host1:27017"))
                .isNotNull()
                .satisfies(assertServer("host1:27017"))
                .satisfies(assertCredentials("graylog", "user@domain", "password"));
    }

    @Test
    void testResolveWithDifferentHosts() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://user:pass@localhost:27017/graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        // Test with different host formats
        assertThat(resolver.resolve("host1:27017"))
                .isNotNull()
                .satisfies(assertServer("host1:27017"))
                .satisfies(assertCredentials("graylog", "user", "pass"));

        assertThat(resolver.resolve("192.168.1.1:27017"))
                .isNotNull()
                .satisfies(assertServer("192.168.1.1:27017"))
                .satisfies(assertCredentials("graylog", "user", "pass"));

        assertThat(resolver.resolve("host.domain.com:27018"))
                .isNotNull()
                .satisfies(assertServer("host.domain.com:27018"))
                .satisfies(assertCredentials("graylog", "user", "pass"));
    }

    @Test
    void testResolveWithIPv6Host() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://user:pass@localhost:27017/graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        assertThat(resolver.resolve("[2001:db8::1]:27017"))
                .isNotNull()
                .satisfies(assertServer("[2001:db8::1]:27017"))
                .satisfies(assertCredentials("graylog", "user", "pass"));
    }

    @Test
    void testResolveUsesSameDatabaseAsOriginalConnection() throws Exception {
        // Original connection uses "testdb" as the database
        MongoDbConfiguration config = createConfiguration("mongodb://user:pass@localhost:27017/testdb");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        assertThat(resolver.resolve("host1:27017"))
                .isNotNull()
                .satisfies(assertServer("host1:27017"))
                .satisfies(assertCredentials("testdb", "user", "pass"));
    }

    @Test
    void testResolveWithEmptyPassword() throws Exception {
        // Username with empty password
        MongoDbConfiguration config = createConfiguration("mongodb://user:@localhost:27017/graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        assertThat(resolver.resolve("host1:27017"))
                .isNotNull()
                .satisfies(assertServer("host1:27017"))
                .satisfies(assertCredentials("graylog", "user", ""));
    }

    @Test
    void testResolvePreservesQueryParameters() throws Exception {
        // Original connection string has query parameters
        MongoDbConfiguration config = createConfiguration("mongodb://user:pass@localhost:27017/graylog?ssl=true&authSource=admin&maxPoolSize=50");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull();

        // Verify the MongoClientOptions reflect the query parameters from the original connection string
        assertThat(client.getMongoClientOptions().isSslEnabled()).isTrue();
        assertThat(client.getMongoClientOptions().getConnectionsPerHost()).isEqualTo(50); // maxPoolSize

        // Verify credentials use the authSource from query params
        assertThat(client.getCredential().getSource()).isEqualTo("admin");
    }

    private MongoDbConfiguration createConfiguration(String uri) throws RepositoryException, ValidationException {
        MongoDbConfiguration config = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_uri", uri)), config).process();
        return config;
    }

    private Consumer<MongoClient> assertServer(String expectedAddress) {
        return (client) -> {
            final ServerDescription serverDescription = client.getClusterDescription()
                    .getServerDescriptions()
                    .stream()
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
            Assertions.assertThat(serverDescription.getAddress()).isEqualTo(new ServerAddress(expectedAddress));
        };
    }

    private Consumer<MongoClient> assertCredentials(String database, String expectedUser, String expectedPassword) {
        return (client) -> {
            final MongoCredential credential = client.getCredential();
            assertThat(credential.getUserName()).isEqualTo(expectedUser);
            assertThat(credential.getSource()).isEqualTo(database);
            assertThat(credential.getPassword()).isEqualTo(expectedPassword.toCharArray());
        };
    }
}
