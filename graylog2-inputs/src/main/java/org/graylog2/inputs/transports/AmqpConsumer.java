/**
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
 */
package org.graylog2.inputs.transports;

import com.google.common.util.concurrent.Uninterruptibles;
import com.rabbitmq.client.*;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AmqpConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpConsumer.class);

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

    private Connection connection;
    private Channel channel;

    private final MessageInput sourceInput;
    private final int parallelQueues;

    private AtomicLong totalBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesReadTmp = new AtomicLong(0);

    public AmqpConsumer(String hostname, int port, String virtualHost, String username, String password,
                        int prefetchCount, String queue, String exchange, String routingKey, int parallelQueues,
                        MessageInput sourceInput, ScheduledExecutorService scheduler) {
        this.hostname = hostname;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.prefetchCount = prefetchCount;

        this.queue = queue;
        this.exchange = exchange;
        this.routingKey = routingKey;

        this.sourceInput = sourceInput;
        this.parallelQueues = parallelQueues;

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                lastSecBytesRead.set(lastSecBytesReadTmp.getAndSet(0));
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void run() throws IOException {
        if (!isConnected()) {
            connect();
        }

        for (int i = 0; i < parallelQueues; i++) {
            final String queueName = String.format(queue, i);
            channel.queueDeclare(queueName, true, false, false, null);
            channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();
                    try {
                        totalBytesRead.addAndGet(body.length);
                        lastSecBytesReadTmp.addAndGet(body.length);

                        final RawMessage rawMessage = new RawMessage("radio-msgpack", sourceInput.getId(), null, body);
                        sourceInput.processRawMessageFailFast(rawMessage);
                        channel.basicAck(deliveryTag, false);
                    } catch (BufferOutOfCapacityException e) {
                        LOG.debug("Input buffer full, requeuing message. Delaying 10 ms until trying next message.");
                        if (channel.isOpen()) {
                            channel.basicNack(deliveryTag, false, true);
                            Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS); // TODO magic number
                        }
                    } catch (ProcessingDisabledException e) {
                        LOG.debug("Message processing is disabled, requeuing message. Delaying 100 ms until trying next message.");
                        if (channel.isOpen()) {
                            channel.basicNack(deliveryTag, false, true);
                            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS); // TODO magic number
                        }
                    } catch (Exception e) {
                        LOG.error("Error while trying to process AMQP message, requeuing message", e);
                        if (channel.isOpen()) {
                            channel.basicNack(deliveryTag, false, true);
                        }
                    }
                }
            });
        }
    }

    public void connect() throws IOException {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostname);
        factory.setPort(port);
        factory.setVirtualHost(virtualHost);

        // Authenticate?
        if(!isNullOrEmpty(username) && !isNullOrEmpty(password)) {
            factory.setUsername(username);
            factory.setPassword(password);
        }

        connection = factory.newConnection();
        channel = connection.createChannel();

        if(null == channel) {
            LOG.error("No channel descriptor available!");
        }

        if (null != channel && prefetchCount > 0) {
            channel.basicQos(prefetchCount);

            LOG.info("AMQP prefetch count overriden to <{}>.", prefetchCount);
        }

        connection.addShutdownListener(new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException cause) {
                if (cause.isInitiatedByApplication()) {
                    LOG.info("Not reconnecting connection, we disconnected explicitly.");
                    return;
                }
                while (true) {
                    try {
                        LOG.error("AMQP connection lost! Trying reconnect in 1 second.");

                        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

                        connect();

                        LOG.info("Connected! Re-starting consumer.");

                        run();

                        LOG.info("Consumer running.");
                        break;
                    } catch(IOException e) {
                        LOG.error("Could not re-connect to AMQP broker.", e);
                    }
                }
            }
        });
    }


    public void stop() throws IOException {
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
