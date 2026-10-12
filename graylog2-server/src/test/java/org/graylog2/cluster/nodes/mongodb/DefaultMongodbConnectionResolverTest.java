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
import com.mongodb.AuthenticationMechanism;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.TaggableReadPreference;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ServerDescription;
import org.assertj.core.api.Assertions;
import org.graylog2.configuration.MongoDbConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Consumer;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;

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

    @Test
    void testResolveWithEncodedSeparatorInOptionValue() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://user:pass@localhost:27017/graylog?appName=graylog%3Dprod&authSource=admin");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("host1:27017"));
        assertThat(client.getMongoClientOptions().getApplicationName()).isEqualTo("graylog=prod");
        assertThat(client.getCredential().getSource()).isEqualTo("admin");
    }

    @Test
    void testResolveWithRepeatedOption() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://localhost:27017/graylog?readPreference=secondaryPreferred&readPreferenceTags=dc:east&readPreferenceTags=");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("host1:27017"));
        assertThat(client.getMongoClientOptions().getReadPreference())
                .isInstanceOfSatisfying(TaggableReadPreference.class, readPreference ->
                        assertThat(readPreference.getTagSetList()).hasSize(2));
    }

    @Test
    void testResolveWithTrailingOptionSeparator() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://user:pass@localhost:27017/graylog?authSource=admin&");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("host1:27017"));
        assertThat(client.getCredential().getSource()).isEqualTo("admin");
    }

    @Test
    void testResolveConnectsDirectlyToReplicaSetMember() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://localhost:27017,localhost:27018/graylog?replicaSet=rs0&directConnection=false");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("host1:27017"));
        assertThat(client.getMongoClientOptions().getRequiredReplicaSetName()).isNull();
        assertThat(client.getClusterDescription().getConnectionMode()).isEqualTo(ClusterConnectionMode.SINGLE);
    }

    @Test
    void testResolveWithLdapReplicaSetConnectionString() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://grayloguser:secret@mongodb1,mongodb2,mongodb3/graylog?tls=true&replicaSet=rs0&readPreference=secondaryPreferred&authMechanism=PLAIN&authSource=%24external");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("mongodb2:27017");
        assertThat(client).isNotNull().satisfies(assertServer("mongodb2:27017"));
        assertThat(client.getClusterDescription().getConnectionMode()).isEqualTo(ClusterConnectionMode.SINGLE);
        assertThat(client.getMongoClientOptions().getRequiredReplicaSetName()).isNull();
        assertThat(client.getMongoClientOptions().isSslEnabled()).isTrue();
        assertThat(client.getMongoClientOptions().getReadPreference()).isEqualTo(ReadPreference.secondaryPreferred());
        assertThat(client.getCredential().getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.PLAIN);
        assertThat(client.getCredential().getSource()).isEqualTo("$external");
        assertThat(client.getCredential().getUserName()).isEqualTo("grayloguser");
        assertThat(client.getCredential().getPassword()).isEqualTo("secret".toCharArray());
    }

    /**
     * Credentials the MongoDB driver accepts unencoded in mongodb_uri, but which are not valid in a java.net.URI.
     * The main connection works with them, so the per-node connections must too.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '\'', value = {
            "grayloguser | 'pass word'",
            "grayloguser | p^ss",
            "grayloguser | p`ss",
            "grayloguser | p{ss}",
            "grayloguser | p\\ss",
            "grayloguser | p\"ss",
            "grayloguser | p<ss>",
            "grayloguser | p[ss]",
            "grayloguser | p#ss",
            "grayloguser | p=ss&x;y,z+w",
            "grayloguser | p!s$s*",
            "DOMAIN\\grayloguser | secret",
            "'cn=graylog,ou=services,dc=example,dc=com' | secret",
    })
    void testResolveWithUnencodedSpecialCharactersInCredentials(String username, String password) throws Exception {
        MongoDbConfiguration config = createConfiguration(f("mongodb://%s:%s@mongodb1,mongodb2/graylog?replicaSet=rs0&authMechanism=PLAIN&authSource=%%24external", username, password));
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("mongodb1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("mongodb1:27017"));
        assertThat(client.getCredential().getUserName()).isEqualTo(username);
        assertThat(client.getCredential().getPassword()).isEqualTo(password.toCharArray());
        assertThat(client.getCredential().getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.PLAIN);
        assertThat(client.getCredential().getSource()).isEqualTo("$external");
    }

    /**
     * URL-encoded credentials must stay encoded in the per-node connection string. Especially ':' and '@' are
     * rejected by the MongoDB driver if they appear decoded in the credentials.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '\'', value = {
            "grayloguser | pa%3Ass | grayloguser | pa:ss",
            "grayloguser | pa%40ss | grayloguser | pa@ss",
            "grayloguser | a%3Ab%40c%2Fd%3Fe%23f%25g%26h%3Di | grayloguser | a:b@c/d?e#f%g&h=i",
            "grayloguser | pa%20ss%2B | grayloguser | 'pa ss+'",
            "grayloguser | p%C3%A4%E2%82%ACss | grayloguser | pä€ss",
            "user%3Agraylog | secret | user:graylog | secret",
            "graylog%40corp.example | secret | graylog@corp.example | secret",
    })
    void testResolveWithEncodedCredentials(String encodedUsername, String encodedPassword, String username, String password) throws Exception {
        MongoDbConfiguration config = createConfiguration(f("mongodb://%s:%s@mongodb1,mongodb2/graylog?tls=true&replicaSet=rs0&authMechanism=PLAIN&authSource=%%24external", encodedUsername, encodedPassword));
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("mongodb1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("mongodb1:27017"));
        assertThat(client.getCredential().getUserName()).isEqualTo(username);
        assertThat(client.getCredential().getPassword()).isEqualTo(password.toCharArray());
        assertThat(client.getCredential().getSource()).isEqualTo("$external");
    }

    @Test
    void testResolveWithUsernameOnly() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://CN%3Dgraylog%2COU%3Dservices@localhost:27017/graylog?tls=true&authMechanism=MONGODB-X509");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("host1:27017"));
        assertThat(client.getCredential().getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.MONGODB_X509);
        assertThat(client.getCredential().getUserName()).isEqualTo("CN=graylog,OU=services");
    }

    @Test
    void testResolveDropsOverriddenOptionsCaseInsensitively() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://localhost:27017,localhost:27018/graylog?REPLICASET=rs0&DirectConnection=false&appName=graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("host1:27017"));
        assertThat(client.getMongoClientOptions().getRequiredReplicaSetName()).isNull();
        assertThat(client.getClusterDescription().getConnectionMode()).isEqualTo(ClusterConnectionMode.SINGLE);
        assertThat(client.getMongoClientOptions().getApplicationName()).isEqualTo("graylog");
    }

    @Test
    void testResolveWithSemicolonOptionSeparator() throws Exception {
        MongoDbConfiguration config = createConfiguration("mongodb://user:pass@localhost:27017/graylog?authSource=admin;appName=graylog");
        MongodbConnectionResolver resolver = new DefaultMongodbConnectionResolver(config, shutdownService);

        MongoClient client = resolver.resolve("host1:27017");
        assertThat(client).isNotNull().satisfies(assertServer("host1:27017"));
        assertThat(client.getCredential().getSource()).isEqualTo("admin");
        assertThat(client.getMongoClientOptions().getApplicationName()).isEqualTo("graylog");
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
