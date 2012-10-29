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
package org.graylog2.inputs.amqp;

import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.yammer.metrics.Metrics;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.activities.Activity;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPReconnector implements ShutdownListener {
    
    private static final Logger LOG = Logger.getLogger(AMQPReconnector.class);
    
    public static final int DELAY_SECONDS = 5;

    private Core server;
    private AMQPQueueConfiguration queueConfig;
    
    public AMQPReconnector(Core server, AMQPQueueConfiguration config) {
        this.server = server;
        this.queueConfig = config;
    }

    @Override
    public void shutdownCompleted(ShutdownSignalException cause) {
        // Only try to re-connect if this queue is still in the configuration.
        if (AMQPQueueConfiguration.fetchAllIds(server).contains(queueConfig.getId())) {
            Metrics.newMeter(AMQPReconnector.class, "ReconnectionAttempts", "reconnections", TimeUnit.SECONDS).mark();
            
            String msg = "Looks like we lost connection to the AMQP broker. "
                    + "Trying to reconnect to " + this.queueConfig + " in " + DELAY_SECONDS + " seconds";
            LOG.error(msg);
            server.getActivityWriter().write(new Activity(msg, AMQPReconnector.class));

            try {
               Thread.sleep(DELAY_SECONDS*1000);
            } catch(InterruptedException ie) {}

            AMQPConsumer consumer = new AMQPConsumer(server, queueConfig);
            AMQPInput.getThreadPool().submit(consumer);
            AMQPInput.getConsumers().put(queueConfig.getId(), consumer);
        } else {
            String msg = "Not trying to reconnect to queue " + queueConfig + ", which is not in the configuration anymore.";
            LOG.info(msg);
            server.getActivityWriter().write(new Activity(msg, AMQPReconnector.class));
        }
    }
    
}