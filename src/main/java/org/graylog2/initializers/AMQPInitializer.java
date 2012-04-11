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

package org.graylog2.initializers;

import java.util.List;
import org.apache.log4j.Logger;
import org.graylog2.Configuration;
import org.graylog2.GraylogServer;
import org.graylog2.messagehandlers.amqp.AMQPBroker;
import org.graylog2.messagehandlers.amqp.AMQPSubscribedQueue;
import org.graylog2.messagehandlers.amqp.AMQPSubscriberThread;

/**
 * AMQPInitializer.java: 11.04.2012 19:21:01
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPInitializer implements Initializer {

    private static final Logger LOG = Logger.getLogger(AMQPInitializer.class);

    private GraylogServer graylogServer;
    private Configuration configuration;

    public AMQPInitializer(GraylogServer graylogServer, Configuration configuration) {
        this.graylogServer = graylogServer;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        // Connect to AMQP broker.
        AMQPBroker amqpBroker = new AMQPBroker(
                configuration.getAmqpHost(),
                configuration.getAmqpPort(),
                configuration.getAmqpUsername(),
                configuration.getAmqpPassword(),
                configuration.getAmqpVirtualhost()
        );

        List<AMQPSubscribedQueue> amqpQueues = configuration.getAmqpSubscribedQueues();

        if (amqpQueues != null) {
            // Start AMQP subscriber thread for each queue to listen on.
            for (AMQPSubscribedQueue queue : amqpQueues) {
                AMQPSubscriberThread amqpThread = new AMQPSubscriberThread(this.graylogServer, queue, amqpBroker);
                amqpThread.start();
            }

            LOG.info("AMQP threads started. (" + amqpQueues.size() + " queues)");
        }
    }

}