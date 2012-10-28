/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.periodical;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.inputs.amqp.AMQPConsumer;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.amqp.AMQPQueueConfiguration;

/**
 * Syncs AMQP consumers with settings. Checks for new or deleted AMQP queue
 * configurations and starts or stops consumers.
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPSyncThread implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(AMQPSyncThread.class);
    
    public static final int INITIAL_DELAY = 5;
    public static final int PERIOD = 5;
    
    private final Core graylogServer;
    
    public AMQPSyncThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void run() {
        Set<AMQPQueueConfiguration> configs = AMQPQueueConfiguration.fetchAll(graylogServer);
        
        // Do we have to spawn new consumers?
        for (AMQPQueueConfiguration config : configs) {
            if (!AMQPInput.getConsumers().containsKey(config.getId())) {
                // This queue config has no consumers yet. Start one.
                LOG.info("Spawning AMQP consumer for new AMQP queue config <" + config + ">");
                AMQPConsumer consumer = new AMQPConsumer(graylogServer, config);
                AMQPInput.getThreadPool().submit(consumer);
                AMQPInput.getConsumers().put(config.getId(), consumer);
            }
        }
            
        // Check if we have to stop consumers.
        if (configs.size() < AMQPInput.getConsumers().size()) {
            LOG.info("Current list of consumers is bigger than configuration. Finding out which to kill.");
            
            for (Map.Entry<String, AMQPConsumer> consumer : AMQPInput.getConsumers().entrySet()) {
                if (!AMQPQueueConfiguration.fetchAllIds(graylogServer).contains(consumer.getKey())) {
                    LOG.info("Consumer <" + consumer + "> is not in the configuratio anymore. Stopping it.");
                    consumer.getValue().disconnect();
                }
            } 
        }
            
    }

}