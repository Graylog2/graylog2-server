package org.graylog2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;

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

        // Additional numerical properties
        validProperties.put("mongodb_max_connections", "100");
        validProperties.put("mongodb_threads_allowed_to_block_multiplier", "50");
        validProperties.put("amqp_port", "5672");
        validProperties.put("forwarder_loggly_timeout", "3");
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
    public void testForceSyslogRdns() throws RepositoryException, ValidationException {
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(false, configuration.getForceSyslogRdns());
    }

    @Test
    public void testGetElasticSearchIndexName() throws RepositoryException, ValidationException {

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals("graylog2", configuration.getElasticSearchIndexName());
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

    @Test
    public void testGetLogglyTimeout() throws RepositoryException, ValidationException {

        validProperties.put("forwarder_loggly_timeout", "5");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(5000, configuration.getForwarderLogglyTimeout());
    }

    @Test
    public void testGetLogglyTimeoutDefault() throws RepositoryException, ValidationException {

        validProperties.remove("forwarder_loggly_timeout");
        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        Assert.assertEquals(3000, configuration.getForwarderLogglyTimeout());
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

        Assert.assertEquals("gl2", configuration.getLibratoMetricsPrefix());
    }
}
