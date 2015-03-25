/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void testGetMongoDBReplicaSetServersEmpty() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "")), configuration).process();

        assertNull(configuration.getReplicaSet());
    }

    @Test
    public void testGetMongoDBReplicaSetServersMalformed() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "malformed#!#")), configuration).process();

        Assert.assertNull(configuration.getReplicaSet());
    }

    @Test
    public void testGetMongoDBReplicaSetServersUnknownHost() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "this-host-hopefully-does-not-exist.:27017")), configuration).process();

        Assert.assertNull(configuration.getReplicaSet());
    }

    @Test
    public void testGetMongoDBReplicaSetServersMalformedPort() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "127.0.0.1:HAHA")), configuration).process();

        Assert.assertNull(configuration.getReplicaSet());
    }

    @Test
    public void testGetMongoDBReplicaSetServersDefaultPort() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "127.0.0.1")), configuration).process();

        assertEquals(configuration.getReplicaSet().get(0).getPort(), 27017);
    }

    @Test
    public void testGetMongoDBReplicaSetServers() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "127.0.0.1:27017,127.0.0.1:27018")), configuration).process();

        assertEquals(2, configuration.getReplicaSet().size());
    }

    @Test
    public void testGetMongoDBReplicaSetServersIPv6() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "fe80::221:6aff:fe6f:6c88,[fe80::221:6aff:fe6f:6c89]:27018,127.0.0.1:27019")), configuration).process();

        assertEquals(3, configuration.getReplicaSet().size());
    }

    @Test(expected = ValidationException.class)
    public void testValidateMongoDbAuth() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_useauth", "true")), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void validateFailsIfUriAndHostAreMissing() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final ImmutableMap<String, String> config = ImmutableMap.of(
                "mongodb_host", "",
                "mongodb_database", "graylog"
        );
        new JadConfig(new InMemoryRepository(config), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void validateFailsIfUriAndDatabaseAreMissing() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "mongodb_host", "localhost",
                "mongodb_database", ""
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();
    }

    @Test
    public void validateSucceedsIfUriIsEmpty() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_uri", "")), configuration).process();
        assertEquals("mongodb://127.0.0.1:27017/graylog2", configuration.getUri());
    }

    @Test
    public void validateSucceedsIfUriIsNull() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_uri", (String) null)), configuration).process();
        assertEquals("mongodb://127.0.0.1:27017/graylog2", configuration.getUri());
    }

    @Test
    public void getMongoClientURIBuildsDatabaseCorrectly() throws Exception {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "mongodb_database", "TEST1234"
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertEquals("mongodb://127.0.0.1:27017/TEST1234", configuration.getMongoClientURI().toString());
    }

    @Test
    public void getMongoClientURIBuildsSingleHostCorrectly() throws Exception {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "mongodb_host", "localhost",
                "mongodb_database", "graylog"
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertEquals("mongodb://localhost:27017/graylog", configuration.getMongoClientURI().toString());
    }

    @Test
    public void getMongoClientURIBuildsSingleHostWithCustomPortCorrectly() throws Exception {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "mongodb_host", "localhost",
                "mongodb_port", "12345",
                "mongodb_database", "graylog"
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertEquals("mongodb://localhost:12345/graylog", configuration.getMongoClientURI().toString());
    }

    @Test
    public void getMongoClientURIBuildsReplicaSetCorrectly() throws Exception {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "mongodb_replica_set", "localhost:1234,localhost:5678,localhost:9012",
                "mongodb_database", "graylog"
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertEquals("mongodb://localhost:1234,localhost:5678,localhost:9012/graylog", configuration.getMongoClientURI().toString());
    }

    @Test
    public void existingUriTakesPrecedenceInGetMongoClientURI() throws Exception {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        final ImmutableMap<String, String> properties = ImmutableMap.of(
                "mongodb_host", "localhost",
                "mongodb_port", "27017",
                "mongodb_database", "graylog",
                "mongodb_uri", "mongodb://example.com:1234,127.0.0.1:5678/TEST"
        );
        new JadConfig(new InMemoryRepository(properties), configuration).process();

        assertEquals("mongodb://example.com:1234,127.0.0.1:5678/TEST", configuration.getMongoClientURI().toString());
    }
}
