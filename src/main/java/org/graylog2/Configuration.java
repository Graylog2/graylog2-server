/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2;

import com.mongodb.ServerAddress;
import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.amqp.AMQPSubscribedQueue;
import org.graylog2.messagehandlers.amqp.InvalidQueueTypeException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Helper class to hold configuration of Graylog2
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    // Define required configuration fields.
    private static final String[] requiredProperties = {
            "syslog_listen_port",
            "syslog_protocol",
            "mongodb_useauth",
            "mongodb_user",
            "mongodb_password",
            "mongodb_database",
            "mongodb_port",
            "messages_collection_size",
            "use_gelf",
            "gelf_listen_port",
    };

    private static final String[] allowedSyslogProtocols = { "tcp", "udp" };
    private static final String[] deprecatedProperties = { "rrd_storage_dir" };
    private static final String[] numericalPositiveProperties = {
            "mongodb_port",
            "mongodb_max_connections",
            "mongodb_threads_allowed_to_block_multiplier",
            "messages_collection_size",
            "gelf_listen_port",
            "syslog_listen_port",
            "amqp_port",
            "forwarder_loggly_timeout",
    };


    private Properties properties;

    public Configuration(Properties properties) {

        if(properties == null) {
            throw new IllegalArgumentException("Properties must not be null");
        }

        this.properties = properties;
    }

    public List<ServerAddress> getMongoDBReplicaSetServers() {
        List<ServerAddress> replicaServers = new ArrayList<ServerAddress>();

        String rawSet = get("mongodb_replica_set");

        if (rawSet == null || rawSet.isEmpty()) {
            return null;
        }

        // Get every host:port pair
        String[] hosts = rawSet.split(",");

        for (String host : hosts) {
            // Split host:port.
            String[] replicaTarget = host.split(":");

            // Check if valid.
            if (replicaTarget == null || replicaTarget.length != 2) {
                LOG.error("Malformed mongodb_replica_set configuration.");
                return null;
            }

            // Get host and port.
            try {
                replicaServers.add(new ServerAddress(replicaTarget[0], Integer.parseInt(replicaTarget[1])));
            } catch (UnknownHostException e) {
                LOG.error("Unknown host in mongodb_replica_set: " + e.getMessage(), e);
                return null;
            }
        }

        return replicaServers;
    }

    public int getMaximumMongoDBConnections() {
        return getInteger("mongodb_max_connections", 1000);
    }


    public int getThreadsAllowedToBlockMultiplier() {
        return getInteger("mongodb_threads_allowed_to_block_multiplier", 5);
    }

    public int getLogglyTimeout() {
        int timeout = getInteger("forwarder_loggly_timeout", 3);

        return timeout*1000;
    }

    public List<AMQPSubscribedQueue> getAMQPSubscribedQueues() {
        List<AMQPSubscribedQueue> queueList = new ArrayList<AMQPSubscribedQueue>();

        String rawQueues = get("amqp_subscribed_queues");

        if (rawQueues == null || rawQueues.isEmpty()) {
            return null;
        }

        // Get every queue.
        String[] queues = rawQueues.split(",");
        for (String queue : queues) {
            String[] queueDefinition = queue.split(":");

            // Check if valid.
            if (queueDefinition == null || queueDefinition.length != 2) {
                LOG.error("Malformed amqp_subscribed_queues configuration.");
                return null;
            }
            try {
                queueList.add(new AMQPSubscribedQueue(queueDefinition[0], queueDefinition[1]));
            } catch (InvalidQueueTypeException e) {
                LOG.error("Invalid queue type in amqp_subscribed_queues");
                return null;
            }
        }

        return queueList;
    }

    public void validate() throws ConfigurationException {

        for (String requiredProperty : requiredProperties) {
            String value = get(requiredProperty);

            if (value == null || value.isEmpty()) {
                throw new ConfigurationException("Mandatory configuration option " + requiredProperty + " not set.");
            }
        }

        // Check if numerical properties are positive
        for (String property : numericalPositiveProperties) {

            int value = getInteger(property, 0);

            if (value < 0) {
                throw new ConfigurationException("Configuration option " + property + " must be a positive integer.");
            }
        }

        // Check if a MongoDB replica set or host is defined.
        if (!contains("mongodb_host") && !contains("mongodb_replica_set")) {
            throw new ConfigurationException("Neither MongoDB host (mongodb_host) nor replica set (mongodb_replica_set) has been defined.");
        }

        // Is the syslog_procotol valid?
        if(!Arrays.asList(allowedSyslogProtocols).contains(get("syslog_protocol"))) {
            throw new ConfigurationException("Invalid syslog_protocol: " + get("syslog_protocol"));
        }

        // Print out a deprecation warning if any deprecated configuration option is set.
        for (String deprecatedProperty : deprecatedProperties) {

            if (get(deprecatedProperty) != null) {
                LOG.warn("Configuration option " + deprecatedProperty + " has been deprecated.");
            }
        }
    }

    public String get(String property) {

        return properties.getProperty(property);
    }

    public int getInteger(String property, int defaultValue) {

        String value = get(property);
        int result = defaultValue;

        if(value != null) {
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                LOG.warn("Couldn't convert configuration property " + property + " to Integer", ex);
            }
        }

        return result;
    }

    public long getLong(String property, long defaultValue) {

        String value = get(property);
        long result = defaultValue;

        if(value != null) {
            try {
                result = Long.parseLong(value);
            } catch (NumberFormatException ex) {
                LOG.warn("Couldn't convert configuration property " + property + " to Long", ex);
            }
        }

        return result;
    }

    public boolean contains(String property) {
        return properties.containsKey(property);
    }

    public boolean getBoolean(String property) {

        return Boolean.parseBoolean(get(property));
    }
}