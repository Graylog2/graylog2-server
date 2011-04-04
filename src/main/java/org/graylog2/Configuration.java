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
import java.util.List;
import java.util.Properties;

/**
 * Configuration.java: Oct 22, 2010 10:57:48 PM
 *
 * This will become the general class to fetch configuration from MongoDB or graylog2.conf with.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    public static List<ServerAddress> getMongoDBReplicaSetServers(Properties config) {
        List<ServerAddress> replicaServers = new ArrayList<ServerAddress>();

        String rawSet = config.getProperty("mongodb_replica_set");

        if (rawSet == null || rawSet.isEmpty()) {
            return null;
        }

        // Get every host:port pair
        String[] hosts = rawSet.split(",");
        for (int i = 0; i < hosts.length; i++) {
            // Split host:port.
            String[] replicaTarget = hosts[i].split(":");

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

    public static int getMaximumMongoDBConnections(Properties config) {
        String val = config.getProperty("mongodb_max_connections");
        if (val != null) {
            int res = Integer.parseInt(val);
            if (res > 0) {
                return res;
            }
        }

        // Default value.
        return 1000;
    }


    public static int getThreadsAllowedToBlockMultiplier(Properties config) {
        String val = config.getProperty("mongodb_threads_allowed_to_block_multiplier");
        if (val != null) {
            int res = Integer.parseInt(val);
            if (res > 0) {
                return res;
            }
        }

        // Default value.
        return 5;
    }

    public static int getLogglyTimeout(Properties config) {
        String val = config.getProperty("forwarder_loggly_timeout");
        if (val != null) {
            int res = Integer.parseInt(val);
            if (res > 0) {
                return res*1000;
            }
        }

        // Default value.
        return 3000;
    }

    static List<AMQPSubscribedQueue> getAMQPSubscribedQueues(Properties config) {
        List<AMQPSubscribedQueue> queueList = new ArrayList<AMQPSubscribedQueue>();

        String rawQueues = config.getProperty("amqp_subscribed_queues");

        if (rawQueues == null || rawQueues.isEmpty()) {
            return null;
        }

        // Get every queue.
        String[] queues = rawQueues.split(",");
        for (int i = 0; i < queues.length; i++) {
            String[] queueDefinition = queues[i].split(":");

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
            } catch (Exception e) {
                LOG.error("Could not parse amqp_subscribed_queues: " + e.getMessage(), e);
                return null;
            }
        }

        return queueList;
    }

}