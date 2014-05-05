/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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

import com.rabbitmq.client.*;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RadioMessage;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Consumer {

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    // Not threadsafe!

    private final String hostname;
    private final int port;
    private final String virtualHost;
    private final String username;
    private final String password;
    private final int prefetchCount;

    private final String queue;
    private final String exchange;
    private final String routingKey;

    private boolean stopped = false;

    private Connection connection;
    private Channel channel;

    private final InputHost graylogServer;
    private final MessageInput sourceInput;

    private AtomicLong totalBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesReadTmp = new AtomicLong(0);

    public Consumer(String hostname, int port, String virtualHost, String username, String password,
                    int prefetchCount, String queue, String exchange, String routingKey,
                    InputHost graylogServer, MessageInput sourceInput) {
        this.hostname = hostname;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.prefetchCount = prefetchCount;

        this.queue = queue;
        this.exchange = exchange;
        this.routingKey = routingKey;

        this.graylogServer = graylogServer;
        this.sourceInput = sourceInput;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                lastSecBytesRead.set(lastSecBytesReadTmp.get());
                lastSecBytesReadTmp.set(0);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void run() throws IOException {
        if (!isConnected()) {
            connect();
        }

        final MessagePack msgpack = new MessagePack();

        channel.basicConsume(queue, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();

                    try {
                        totalBytesRead.addAndGet(body.length);
                        lastSecBytesReadTmp.addAndGet(body.length);

                        RadioMessage msg = msgpack.read(body, RadioMessage.class);

                        if (!msg.strings.containsKey("message") || !msg.strings.containsKey("source") || msg.timestamp <= 0) {
                            LOG.error("Incomplete AMQP message. Skipping.");
                            channel.basicAck(deliveryTag, false);
                        }

                        Message event = new Message(
                                msg.strings.get("message"),
                                msg.strings.get("source"),
                                new DateTime(msg.timestamp)
                        );

                        event.addStringFields(msg.strings);
                        event.addLongFields(msg.longs);
                        event.addDoubleFields(msg.doubles);

                        graylogServer.getProcessBuffer().insertCached(event, sourceInput);

                    } catch (Exception e) {
                        LOG.error("Error while trying to process AMQP message.", e);
                    }

                    channel.basicAck(deliveryTag, false);
                }
            }
        );
    }

    public void connect() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostname);
        factory.setPort(port);

        factory.setVirtualHost(virtualHost);

        // Authenticate?
        if(username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            factory.setUsername(username);
            factory.setPassword(password);
        }

        connection = factory.newConnection();
        channel = connection.createChannel();

        if (prefetchCount > 0) {
            channel.basicQos(prefetchCount);

            LOG.info("AMQP prefetch count overriden to <{}>.", prefetchCount);
        }

        connection.addShutdownListener(new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException cause) {
                while (!stopped) {
                    try {
                        LOG.error("AMQP connection lost! Trying reconnect in 1 second.");

                        Thread.sleep(1000);

                        connect();

                        LOG.info("Connected! Re-starting consumer.");

                        run();

                        LOG.info("Consumer running.");
                        break;
                    } catch(IOException e) {
                        LOG.error("Could not re-connect to AMQP broker.", e);
                    } catch(InterruptedException ignored) {
                    }
                }
            }
        });
    }


    public void stop() throws IOException {
        this.stopped = true; // Disables reconnector.

        if (channel != null && channel.isOpen()) {
            channel.close();
        }

        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }

    public boolean isConnected() {
        return connection != null
                && connection.isOpen()
                && channel != null
                && channel.isOpen();
    }

    public AtomicLong getLastSecBytesRead() {
        return lastSecBytesRead;
    }

    public AtomicLong getTotalBytesRead() {
        return totalBytesRead;
    }
}
