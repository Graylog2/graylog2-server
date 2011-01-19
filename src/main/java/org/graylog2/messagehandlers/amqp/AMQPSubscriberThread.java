/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.coms>
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

package org.graylog2.messagehandlers.amqp;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graylog2.Log;

/**
 * AMQPSubscriberThread.java: Jun 23, 2010 7:19:52 PM
 *
 * Thread responsible for subscribing to AMQP queues.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPSubscriberThread extends Thread {

    /**
     * Run the thread. Runs forever!
     */
    @Override public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        factory.setHost("localhost");
        factory.setPort(5672);
        boolean autoAck = false;

        com.rabbitmq.client.Connection conn = null;
        com.rabbitmq.client.Channel channel = null;
        QueueingConsumer consumer = new QueueingConsumer(channel);
        try {
            conn = factory.newConnection();
            channel = conn.createChannel();
            channel.basicConsume("graylog2-logs", autoAck, consumer);
        } catch (IOException e) {
            Log.crit("Could not connect to AMQP broker or channel: " + e.toString());
        }

        while (true) {
            QueueingConsumer.Delivery delivery;
            try {
                delivery = consumer.nextDelivery();
            } catch (InterruptedException ie) {
                continue;
            }

            System.out.println("HANDLED MESSAGE: " + new String(delivery.getBody()));
            
            try {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (IOException e) {
                Log.crit("Could not ack AMQP message: " + e.toString());
            }

        }
    }

}
