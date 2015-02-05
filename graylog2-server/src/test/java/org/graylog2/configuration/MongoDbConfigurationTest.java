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
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.util.Collections.singletonMap;

public class MongoDbConfigurationTest {
    @Test
    public void testGetMaximumMongoDBConnections() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_max_connections", "12345")), configuration).process();

        Assert.assertEquals(12345, configuration.getMaxConnections());
    }

    @Test
    public void testGetMaximumMongoDBConnectionsDefault() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();

        Assert.assertEquals(1000, configuration.getMaxConnections());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplier() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_threads_allowed_to_block_multiplier", "12345")), configuration).process();

        Assert.assertEquals(12345, configuration.getThreadsAllowedToBlockMultiplier());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplierDefault() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();

        Assert.assertEquals(5, configuration.getThreadsAllowedToBlockMultiplier());
    }

    @Test
    public void testGetMongoDBReplicaSetServersEmpty() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "")), configuration).process();

        Assert.assertNull(configuration.getReplicaSet());
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

        Assert.assertEquals(configuration.getReplicaSet().get(0).getPort(), 27017);
    }

    @Test
    public void testGetMongoDBReplicaSetServers() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "127.0.0.1:27017,127.0.0.1:27018")), configuration).process();

        Assert.assertEquals(2, configuration.getReplicaSet().size());
    }

    @Test
    public void testGetMongoDBReplicaSetServersIPv6() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_replica_set", "fe80::221:6aff:fe6f:6c88,[fe80::221:6aff:fe6f:6c89]:27018,127.0.0.1:27019")), configuration).process();

        Assert.assertEquals(3, configuration.getReplicaSet().size());
    }


    @Test(expectedExceptions = ValidationException.class)
    public void testValidateMongoDbAuth() throws RepositoryException, ValidationException {
        MongoDbConfiguration configuration = new MongoDbConfiguration();
        new JadConfig(new InMemoryRepository(singletonMap("mongodb_useauth", "true")), configuration).process();
    }
}
