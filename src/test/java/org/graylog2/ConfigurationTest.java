package org.graylog2;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

/**
 * Unit tests for {@link Configuration} class
 *
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class ConfigurationTest {

    Properties validProperties;

    @Before
    public void setUp() {

        validProperties = new Properties();

        // Required properties
        validProperties.setProperty("syslog_listen_port", "514");
        validProperties.setProperty("syslog_protocol", "udp");
        validProperties.setProperty("mongodb_useauth", "true");
        validProperties.setProperty("mongodb_user", "user");
        validProperties.setProperty("mongodb_password", "pass");
        validProperties.setProperty("mongodb_database", "test");
        validProperties.setProperty("mongodb_host", "localhost");
        validProperties.setProperty("mongodb_port", "27017");
        validProperties.setProperty("messages_collection_size", "1000");
        validProperties.setProperty("use_gelf", "true");
        validProperties.setProperty("gelf_listen_port", "12201");

        // Additional numerical properties
        validProperties.setProperty("mongodb_max_connections", "100");
        validProperties.setProperty("mongodb_threads_allowed_to_block_multiplier", "50");
        validProperties.setProperty("amqp_port", "5672");
        validProperties.setProperty("forwarder_loggly_timeout", "3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPropertiesNull() {

        new Configuration(null);
    }

    @Test
    public void testContains() {

        Properties properties = new Properties();
        properties.setProperty("thisIsNotNull", "notNull");
        properties.setProperty("thisIsEmpty", "");

        Configuration configuration = new Configuration(properties);

        Assert.assertTrue(configuration.contains("thisIsNotNull"));
        Assert.assertTrue(configuration.contains("thisIsEmpty"));
        Assert.assertFalse(configuration.contains("thisDoesNotExist"));
    }

    @Test
    public void testGet() {

        Properties properties = new Properties();
        properties.setProperty("thisIsNotNull", "notNull");
        properties.setProperty("thisIsEmpty", "");

        Configuration configuration = new Configuration(properties);

        Assert.assertEquals("notNull", configuration.get("thisIsNotNull"));
        Assert.assertEquals("", configuration.get("thisIsEmpty"));
        Assert.assertNull(configuration.get("thisIsNull"));
    }

    @Test
    public void testGetInteger() {

        Properties properties = new Properties();
        properties.setProperty("thisIsEmpty", "");
        properties.setProperty("testString", "invalidInteger");
        properties.setProperty("testInteger", "12345");
        properties.setProperty("MIN_VALUE", String.valueOf(Integer.MIN_VALUE));
        properties.setProperty("MAX_VALUE", String.valueOf(Integer.MAX_VALUE));
        properties.setProperty("tooSmall", "-2147483649");
        properties.setProperty("tooBig", "2147483648");

        Configuration configuration = new Configuration(properties);

        Assert.assertEquals(123, configuration.getInteger("thisIsNull", 123));
        Assert.assertEquals(123, configuration.getInteger("thisIsEmpty", 123));
        Assert.assertEquals(123, configuration.getInteger("testString", 123));
        Assert.assertEquals(12345, configuration.getInteger("testInteger", 123));
        Assert.assertEquals(Integer.MIN_VALUE, configuration.getInteger("MIN_VALUE", 123));
        Assert.assertEquals(Integer.MAX_VALUE, configuration.getInteger("MAX_VALUE", 123));
        Assert.assertEquals(123, configuration.getInteger("tooSmall", 123));
        Assert.assertEquals(123, configuration.getInteger("tooBig", 123));
    }

    @Test
    public void testGetLong() {

        Properties properties = new Properties();
        properties.setProperty("thisIsEmpty", "");
        properties.setProperty("testString", "invalidLong");
        properties.setProperty("testInteger", "12345");
        properties.setProperty("testLong", "12345");
        properties.setProperty("MIN_VALUE", String.valueOf(Long.MIN_VALUE));
        properties.setProperty("MAX_VALUE", String.valueOf(Long.MAX_VALUE));
        properties.setProperty("tooSmall", "-9223372036854775809");
        properties.setProperty("tooBig", "9223372036854775808");

        Configuration configuration = new Configuration(properties);

        Assert.assertEquals(123L, configuration.getLong("thisIsNull", 123L));
        Assert.assertEquals(123L, configuration.getLong("thisIsEmpty", 123L));
        Assert.assertEquals(123L, configuration.getLong("testString", 123L));
        Assert.assertEquals(12345L, configuration.getLong("testInteger", 123L));
        Assert.assertEquals(12345L, configuration.getLong("testLong", 123L));
        Assert.assertEquals(Long.MIN_VALUE, configuration.getLong("MIN_VALUE", 123L));
        Assert.assertEquals(Long.MAX_VALUE, configuration.getLong("MAX_VALUE", 123L));
        Assert.assertEquals(123L, configuration.getLong("tooSmall", 123L));
        Assert.assertEquals(123L, configuration.getLong("tooBig", 123L));
    }

    @Test
    public void testGetBoolean() {
        Properties properties = new Properties();
        properties.setProperty("thisIsEmpty", "");
        properties.setProperty("thisIsNotBoolean", "1");
        properties.setProperty("thisIsTrue1", "true");
        properties.setProperty("thisIsTrue2", "TRUE");
        properties.setProperty("thisIsTrue3", "tRuE");

        Configuration configuration = new Configuration(properties);

        Assert.assertFalse(configuration.getBoolean("thisIsEmpty"));
        Assert.assertFalse(configuration.getBoolean("thisIsNotBoolean"));
        Assert.assertTrue(configuration.getBoolean("thisIsTrue1"));
        Assert.assertTrue(configuration.getBoolean("thisIsTrue2"));
        Assert.assertTrue(configuration.getBoolean("thisIsTrue3"));
    }

    @Test(expected = ConfigurationException.class)
    public void testValidateRequiredPropertiesMissing() throws ConfigurationException {

        Properties properties = new Properties();
        Configuration configuration = new Configuration(properties);
        configuration.validate();
    }

    @Test
    public void testValidateNumericalProperties() throws ConfigurationException {

        validProperties.setProperty("mongodb_port", "NotANumber");
        Configuration configuration = new Configuration(validProperties);
        configuration.validate();

        Assert.assertTrue(configuration.contains("mongodb_port"));
    }

    @Test(expected = ConfigurationException.class)
    public void testValidateNeitherMongoDbHostNorReplicaSet() throws ConfigurationException {

        validProperties.remove("mongodb_host");
        Configuration configuration = new Configuration(validProperties);
        configuration.validate();
    }

    @Test
    public void testValidateNoMongoDbHost() throws ConfigurationException {

        validProperties.remove("mongodb_host");
        validProperties.setProperty("mongodb_replica_set", "localhost");

        Configuration configuration = new Configuration(validProperties);
        configuration.validate();

        Assert.assertTrue(configuration.contains("mongodb_replica_set"));
        Assert.assertFalse(configuration.contains("mongodb_host"));
    }

    @Test(expected = ConfigurationException.class)
    public void testValidateSyslogProtocol() throws ConfigurationException {

        validProperties.setProperty("syslog_protocol", "noValidProtocol");

        Configuration configuration = new Configuration(validProperties);
        configuration.validate();
    }

    @Test
    public void testGetMaximumMongoDBConnections() {

        validProperties.setProperty("mongodb_max_connections", "12345");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(12345, configuration.getMaximumMongoDBConnections());
    }

    @Test
    public void testGetMaximumMongoDBConnectionsDefault() {

        validProperties.remove("mongodb_max_connections");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(1000, configuration.getMaximumMongoDBConnections());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplier() {

        validProperties.setProperty("mongodb_threads_allowed_to_block_multiplier", "12345");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(12345, configuration.getThreadsAllowedToBlockMultiplier());
    }

    @Test
    public void testGetThreadsAllowedToBlockMultiplierDefault() {

        validProperties.remove("mongodb_threads_allowed_to_block_multiplier");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(5, configuration.getThreadsAllowedToBlockMultiplier());
    }

    @Test
    public void testGetLogglyTimeout() {

        validProperties.setProperty("forwarder_loggly_timeout", "5");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(5000, configuration.getLogglyTimeout());
    }

    @Test
    public void testGetLogglyTimeoutDefault() {

        validProperties.remove("forwarder_loggly_timeout");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(3000, configuration.getLogglyTimeout());
    }

    @Test
    public void testGetAMQPSubscribedQueuesEmpty() {
        validProperties.setProperty("amqp_subscribed_queues", "");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertNull(configuration.getAMQPSubscribedQueues());
    }

    @Test
    public void testGetAMQPSubscribedQueuesMalformed() {
        validProperties.setProperty("amqp_subscribed_queues", "queue-invalid");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertNull(configuration.getAMQPSubscribedQueues());
    }

    @Test
    public void testGetAMQPSubscribedQueuesInvalidQueueType() {
        validProperties.setProperty("amqp_subscribed_queues", "queue1:gelf,queue2:invalid");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertNull(configuration.getAMQPSubscribedQueues());
    }

    @Test
    public void testGetAMQPSubscribedQueues() {
        validProperties.setProperty("amqp_subscribed_queues", "queue1:gelf,queue2:syslog");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(2, configuration.getAMQPSubscribedQueues().size());
    }

    @Test
    public void testGetMongoDBReplicaSetServersEmpty() {
        validProperties.setProperty("mongodb_replica_set", "");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertNull(configuration.getMongoDBReplicaSetServers());
    }

    @Test
    public void testGetMongoDBReplicaSetServersMalformed() {
        validProperties.setProperty("mongodb_replica_set", "malformed");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertNull(configuration.getMongoDBReplicaSetServers());
    }

    @Test
    public void testGetMongoDBReplicaSetServersUnknownHost() {
        validProperties.setProperty("mongodb_replica_set", "this-host-hopefully-does-not-exist:27017");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertNull(configuration.getMongoDBReplicaSetServers());
    }

    @Test
    public void testGetMongoDBReplicaSetServers() {
        validProperties.setProperty("mongodb_replica_set", "localhost:27017,localhost:27018");
        Configuration configuration = new Configuration(validProperties);

        Assert.assertEquals(2, configuration.getMongoDBReplicaSetServers().size());
    }
}
