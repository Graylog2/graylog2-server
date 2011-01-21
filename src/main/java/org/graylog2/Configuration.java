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
                Log.crit("Malformed mongodb_replica_set configuration.");
                return null;
            }

            // Get host and port.
            for (int j = 0; j < 2; j++) {
                try {
                    replicaServers.add(new ServerAddress(replicaTarget[0], Integer.parseInt(replicaTarget[1])));
                } catch (UnknownHostException e) {
                    Log.crit("Unknown host in mongodb_replica_set: " + e.toString());
                    return null;
                }
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

    static List<String> getAMQPSubscribedQueues(Properties config) {
        List<String> queueList = new ArrayList<String>();

        String rawQueues = config.getProperty("amqp_subscribed_queues");

        if (rawQueues == null || rawQueues.isEmpty()) {
            return null;
        }

        // Get every queue.
        String[] queues = rawQueues.split(",");
        for (int i = 0; i < queues.length; i++) {
            String queue = queues[i];
            // Check if valid.
            if (queue == null || queue.isEmpty()) {
                Log.crit("Malformed amqp_subscribed_queues configuration.");
                return null;
            }

            queueList.add(queues[i]);
        }

        return queueList;
    }

}