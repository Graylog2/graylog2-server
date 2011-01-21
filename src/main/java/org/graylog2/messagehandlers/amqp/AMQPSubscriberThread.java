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

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;
import org.graylog2.Log;

/**
 * AMQPSubscriberThread.java: Jan 20, 2011 7:19:52 PM
 *
 * Thread responsible for subscribing to AMQP queues.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPSubscriberThread extends Thread {

    private AMQPSubscribedQueue queue = null;
    private AMQPBroker broker = null;

    public final static int SLEEP_INTERVAL = 10;

    public AMQPSubscriberThread(AMQPSubscribedQueue queue, AMQPBroker broker) {
        this.queue = queue;
        this.broker = broker;
    }

    /**
     * Run the thread. Runs forever!
     */
    @Override public void run() {
        while(true) {
            Connection connection = null;
            Channel channel = null;
            QueueingConsumer consumer = new QueueingConsumer(channel);
            
            try {
                connection = broker.getConnection();
                channel = connection.createChannel();
                channel.basicConsume(this.queue.getName(), false, consumer);
            } catch (Exception e) {
                Log.crit("AMQP queue '" + this.queue + "': Could not connect to AMQP broker or channel (Make sure that "
                        + "the queue exists. Retrying in " + SLEEP_INTERVAL + " seconds. (" + e.toString() + ")");
                
                // Retry after waiting for SLEEP_INTERVAL seconds.
                try { Thread.sleep(SLEEP_INTERVAL*1000); } catch(InterruptedException foo) {}
                continue;
            }

            while (true) {
                try {
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
                } catch(Exception e) {
                    // Error while receiving. i.e. when AMQP broker breaks down.
                    Log.crit("AMQP queue '" + this.queue + "': Error while subscribed (rebuilding connection "
                            + "in " + SLEEP_INTERVAL + " seconds. (" + e.toString() + ")");

                    // Better close connection stuff it is still active.
                    try {
                        channel.close();
                        connection.close();
                    } catch (IOException ex) {
                        // I don't care.
                    } catch (AlreadyClosedException ex) {
                        // I don't care.
                    }

                    // Retry after waiting for SLEEP_INTERVAL seconds.
                    try { Thread.sleep(SLEEP_INTERVAL*1000); } catch(InterruptedException foo) {}
                    break;
                }
            }
        }
    }

}
