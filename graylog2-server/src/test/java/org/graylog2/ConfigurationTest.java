package org.graylog2;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Unit tests for {@link Configuration} class
 *
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class ConfigurationTest {

    Map<String, String> validProperties;
    private File tempFile;

    @Before
    public void setUp() {

        validProperties = Maps.newHashMap();

        try {
            tempFile = File.createTempFile("graylog", null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Required properties
        validProperties.put("password_secret", "ipNUnWxmBLCxTEzXcyamrdy0Q3G7HxdKsAvyg30R9SCof0JydiZFiA3dLSkRsbLF");
        validProperties.put("elasticsearch_config_file", tempFile.getAbsolutePath());
        validProperties.put("force_syslog_rdns", "false");
        validProperties.put("syslog_listen_port", "514");
        validProperties.put("syslog_protocol", "udp");
        validProperties.put("mongodb_useauth", "true");
        validProperties.put("mongodb_user", "user");
        validProperties.put("mongodb_password", "pass");
        validProperties.put("mongodb_database", "test");
        validProperties.put("mongodb_host", "localhost");
        validProperties.put("mongodb_port", "27017");
        validProperties.put("use_gelf", "true");
        validProperties.put("gelf_listen_port", "12201");
        validProperties.put("root_password_sha2", "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"); // sha2 of admin

        // Additional numerical properties
        validProperties.put("mongodb_max_connections", "100");
        validProperties.put("mongodb_threads_allowed_to_block_multiplier", "50");
        validProperties.put("amqp_port", "5672");
        validProperties.put("forwarder_loggly_timeout", "3");

        validProperties.put("retention_strategy", "delete");
    }

    @After
    public void tearDown() {
        tempFile.delete();
    }

    @Test(expected = ValidationException.class)
    public void testValidateMongoDbAuth() throws RepositoryException, ValidationException {

        validProperties.put("mongodb_useauth", "true");
        validProperties.remove("mongodb_user");
        validProperties.remove("mongodb_password");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testGetElasticSearchIndexPrefix() throws RepositoryException, ValidationException {

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("graylog2", configuration.getElasticSearchIndexPrefix());
    }

    @Test
    public void testGetMaximumMongoDBConnections() throws RepositoryException, ValidationException {

        validProperties.put("mongodb_max_connections", "12345");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(12345, configuration.getMongoMaxConnections());
    }

    @Test
    public void testGetMaximumMongoDBConnectionsDefault() throws RepositoryException, ValidationException {

        validProperties.remove("mongodb_max_connections");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(1000, configuration.getMongoMaxConnections());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplier() throws RepositoryException, ValidationException {

        validProperties.put("mongodb_threads_allowed_to_block_multiplier", "12345");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(12345, configuration.getMongoThreadsAllowedToBlockMultiplier());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplierDefault() throws RepositoryException, ValidationException {

        validProperties.remove("mongodb_threads_allowed_to_block_multiplier");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(5, configuration.getMongoThreadsAllowedToBlockMultiplier());
    }

    /*@Test
    public void testGetAMQPSubscribedQueuesEmpty() throws RepositoryException, ValidationException {
        validProperties.put("amqp_subscribed_queues", "");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNull(configuration.getAmqpSubscribedQueues());
    }

    @Test
    public void testGetAMQPSubscribedQueuesMalformed() throws RepositoryException, ValidationException {
        validProperties.put("amqp_subscribed_queues", "queue-invalid");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNull(configuration.getAmqpSubscribedQueues());
    }

    @Test
    public void testGetAMQPSubscribedQueuesInvalidQueueType() throws RepositoryException, ValidationException {
        validProperties.put("amqp_subscribed_queues", "queue1:gelf,queue2:invalid");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNull(configuration.getAmqpSubscribedQueues());
    }

    @Test
    public void testGetAMQPSubscribedQueues() throws RepositoryException, ValidationException {
        validProperties.put("amqp_subscribed_queues", "queue1:gelf,queue2:syslog");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(2, configuration.getAmqpSubscribedQueues().size());
    }*/

    @Test
    public void testGetMongoDBReplicaSetServersEmpty() throws RepositoryException, ValidationException {
        validProperties.put("mongodb_replica_set", "");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNull(configuration.getMongoReplicaSet());
    }

    @Test
    public void testGetMongoDBReplicaSetServersMalformed() throws RepositoryException, ValidationException {
        validProperties.put("mongodb_replica_set", "malformed");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNull(configuration.getMongoReplicaSet());
    }

    @Test
    public void testGetMongoDBReplicaSetServersUnknownHost() throws RepositoryException, ValidationException {
        validProperties.put("mongodb_replica_set", "this-host-hopefully-does-not-exist.:27017");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertNull(configuration.getMongoReplicaSet());
    }

    @Test
    public void testGetMongoDBReplicaSetServers() throws RepositoryException, ValidationException {
        validProperties.put("mongodb_replica_set", "127.0.0.1:27017,127.0.0.1:27018");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(2, configuration.getMongoReplicaSet().size());
    }

    @Test
    public void testGetLibratoMetricsStreamFilter() throws RepositoryException, ValidationException {
        ObjectId id1 = new ObjectId();
        ObjectId id2 = new ObjectId();
        ObjectId id3 = new ObjectId();
        validProperties.put("libratometrics_stream_filter", id1.toString() + "," + id2.toString() + "," + id3.toString());

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(3, configuration.getLibratoMetricsStreamFilter().size());
        Assert.assertTrue(configuration.getLibratoMetricsStreamFilter().contains(id1.toString()));
        Assert.assertTrue(configuration.getLibratoMetricsStreamFilter().contains(id2.toString()));
        Assert.assertTrue(configuration.getLibratoMetricsStreamFilter().contains(id3.toString()));
    }

    @Test
    public void testGetLibratoMetricsPrefix() throws RepositoryException, ValidationException {
        validProperties.put("libratometrics_prefix", "lolwut");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("lolwut", configuration.getLibratoMetricsPrefix());
    }

    @Test
    public void testGetLibratoMetricsPrefixHasStandardValue() throws RepositoryException, ValidationException {
        // Nothing set.
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("gl2-", configuration.getLibratoMetricsPrefix());
    }
    
}
