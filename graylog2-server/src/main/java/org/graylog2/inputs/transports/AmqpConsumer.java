/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports;

import com.google.common.util.concurrent.Uninterruptibles;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final boolean exchangeBind;
    private final String routingKey;
    private final boolean requeueInvalid;

    private Connection connection;
    private Channel channel;

    private final int heartbeatTimeout;
    private final MessageInput sourceInput;
    private final int parallelQueues;
    private final boolean tls;
    private AmqpTransport amqpTransport;

    private AtomicLong totalBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesReadTmp = new AtomicLong(0);

    public AmqpConsumer(String hostname, int port, String virtualHost, String username, String password,
                        int prefetchCount, String queue, String exchange, boolean exchangeBind, String routingKey,int parallelQueues,
                        boolean tls, boolean requeueInvalid, int heartbeatTimeout, MessageInput sourceInput,
                        ScheduledExecutorService scheduler, AmqpTransport amqpTransport) {
        this.hostname = hostname;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.prefetchCount = prefetchCount;

        this.queue = queue;
        this.exchange = exchange;
        this.exchangeBind = exchangeBind;
        this.routingKey = routingKey;
        this.heartbeatTimeout = heartbeatTimeout;

        this.sourceInput = sourceInput;
        this.parallelQueues = parallelQueues;
        this.tls = tls;
        this.requeueInvalid = requeueInvalid;
        this.amqpTransport = amqpTransport;

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
            final String queueName = String.format(Locale.ENGLISH, queue, i);
            channel.queueDeclare(queueName, true, false, false, null);
            if (exchangeBind) {
                channel.queueBind(queueName, exchange, routingKey);
            }
            channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();
                    try {
                        totalBytesRead.addAndGet(body.length);
                        lastSecBytesReadTmp.addAndGet(body.length);

                        final RawMessage rawMessage = new RawMessage(body);

                        // TODO figure out if we want to unsubscribe after a certain time, or if simply blocking is enough here
                        if (amqpTransport.isThrottled()) {
                            amqpTransport.blockUntilUnthrottled();
                        }

                        sourceInput.processRawMessage(rawMessage);
                        channel.basicAck(deliveryTag, false);
                    } catch (Exception e) {
                        LOG.error("Error while trying to process AMQP message", e);
                        if (channel.isOpen()) {
                            channel.basicNack(deliveryTag, false, requeueInvalid);

                            if (LOG.isDebugEnabled()) {
                                if (requeueInvalid) {
                                    LOG.debug("Re-queue message with delivery tag {}", deliveryTag);
                                } else {
                                    LOG.debug("Message with delivery tag {} not re-queued", deliveryTag);
                                }
                            }
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
        factory.setRequestedHeartbeat(heartbeatTimeout);

        if (tls) {
            try {
                LOG.info("Enabling TLS for AMQP input [{}/{}].", sourceInput.getName(), sourceInput.getId());
                factory.useSslProtocol();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Couldn't enable TLS for AMQP input.", e);
            }
        }

        // Authenticate?
        if (!isNullOrEmpty(username) && !isNullOrEmpty(password)) {
            factory.setUsername(username);
            factory.setPassword(password);
        }

        try {
            connection = factory.newConnection();
        } catch (TimeoutException e) {
            throw new IOException("Timeout while opening new AMQP connection", e);
        }

        channel = connection.createChannel();

        if (null == channel) {
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
                    } catch (IOException e) {
                        LOG.error("Could not re-connect to AMQP broker.", e);
                    }
                }
            }
        });
    }


    public void stop() throws IOException {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (TimeoutException e) {
                LOG.error("Timeout when closing AMQP channel", e);
                channel.abort();
            }
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
