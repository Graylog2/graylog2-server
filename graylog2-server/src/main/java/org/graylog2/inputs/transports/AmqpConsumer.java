/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs.transports;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.TrafficListener;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import com.rabbitmq.client.impl.StandardMetricsCollector;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.MetricSets;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.inputs.transports.AmqpTransport.CK_EXCHANGE;
import static org.graylog2.inputs.transports.AmqpTransport.CK_EXCHANGE_BIND;
import static org.graylog2.inputs.transports.AmqpTransport.CK_HOSTNAME;
import static org.graylog2.inputs.transports.AmqpTransport.CK_PARALLEL_QUEUES;
import static org.graylog2.inputs.transports.AmqpTransport.CK_PASSWORD;
import static org.graylog2.inputs.transports.AmqpTransport.CK_PORT;
import static org.graylog2.inputs.transports.AmqpTransport.CK_PREFETCH;
import static org.graylog2.inputs.transports.AmqpTransport.CK_QUEUE;
import static org.graylog2.inputs.transports.AmqpTransport.CK_QUEUE_DECLARE_PASSIVE;
import static org.graylog2.inputs.transports.AmqpTransport.CK_REQUEUE_INVALID_MESSAGES;
import static org.graylog2.inputs.transports.AmqpTransport.CK_ROUTING_KEY;
import static org.graylog2.inputs.transports.AmqpTransport.CK_TLS;
import static org.graylog2.inputs.transports.AmqpTransport.CK_USERNAME;
import static org.graylog2.inputs.transports.AmqpTransport.CK_VHOST;
import static org.graylog2.shared.utilities.StringUtils.f;

public class AmqpConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpConsumer.class);

    // Not threadsafe!

    private final String hostname;
    private final int port;
    private final String virtualHost;
    private final String username;
    private final EncryptedValue password;
    private final int prefetchCount;

    private final String queue;
    private final boolean queueDeclarePassive;
    private final String exchange;
    private final boolean exchangeBind;
    private final String routingKey;
    private final boolean requeueInvalid;
    private final int heartbeatTimeout;
    private final NodeId nodeId;
    private final MessageInput sourceInput;
    private final int parallelQueues;
    private final boolean tls;
    private final InputFailureRecorder inputFailureRecorder;
    private final AmqpTransport amqpTransport;
    private final EncryptedValueService encryptedValueService;
    private final Duration connectionRecoveryInterval;
    private final StandardMetricsCollector metricsCollector;

    private final AtomicLong totalBytesRead = new AtomicLong(0);
    private final Supplier<AtomicLong> lastSecBytesSupplier = Suppliers.memoizeWithExpiration(
            () -> new AtomicLong(0),
            Duration.ofSeconds(1)
    );

    private Connection connection;
    private Channel channel;
    private ExecutorService executorService;

    public AmqpConsumer(int heartbeatTimeout,
                        NodeId nodeId,
                        MessageInput sourceInput,
                        Configuration configuration,
                        InputFailureRecorder inputFailureRecorder,
                        AmqpTransport amqpTransport,
                        EncryptedValueService encryptedValueService,
                        Duration connectionRecoveryInterval) {
        this.nodeId = nodeId;
        this.hostname = configuration.getString(CK_HOSTNAME);
        this.port = configuration.getInt(CK_PORT);
        this.virtualHost = configuration.getString(CK_VHOST);
        this.username = configuration.getString(CK_USERNAME);
        this.password = configuration.getEncryptedValue(CK_PASSWORD);
        this.prefetchCount = configuration.getInt(CK_PREFETCH);
        this.queue = configuration.getString(CK_QUEUE);
        this.queueDeclarePassive = configuration.getBoolean(CK_QUEUE_DECLARE_PASSIVE);
        this.exchange = configuration.getString(CK_EXCHANGE);
        this.exchangeBind = configuration.getBoolean(CK_EXCHANGE_BIND);
        this.routingKey = configuration.getString(CK_ROUTING_KEY);
        this.parallelQueues = configuration.getInt(CK_PARALLEL_QUEUES);
        this.requeueInvalid = configuration.getBoolean(CK_REQUEUE_INVALID_MESSAGES);
        this.tls = configuration.getBoolean(CK_TLS);
        this.heartbeatTimeout = heartbeatTimeout;
        this.sourceInput = sourceInput;
        this.inputFailureRecorder = inputFailureRecorder;
        this.amqpTransport = amqpTransport;
        this.encryptedValueService = encryptedValueService;
        this.connectionRecoveryInterval = connectionRecoveryInterval;
        this.metricsCollector = new StandardMetricsCollector(new LocalMetricRegistry(), "amqp");
    }

    public void run() throws IOException, TimeoutException {

        if (!isConnected()) {
            connect();
        }

        for (int i = 0; i < parallelQueues; i++) {
            final String queueName = String.format(Locale.ENGLISH, queue, i);

            if (queueDeclarePassive) {
                channel.queueDeclarePassive(queueName);
            } else {
                channel.queueDeclare(queueName, true, false, false, null);
            }

            if (exchangeBind) {
                channel.queueBind(queueName, exchange, routingKey);
            }

            channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();
                    try {
                        totalBytesRead.addAndGet(body.length);
                        lastSecBytesSupplier.get().addAndGet(body.length);

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

    public void connect() throws IOException, TimeoutException {
        if (connection == null) {
            establishConnection();
        }

        channel = connection.createChannel();

        if (null == channel) {
            LOG.error("No channel descriptor available!");
        }

        if (null != channel && prefetchCount > 0) {
            channel.basicQos(prefetchCount);

            LOG.debug("AMQP prefetch count overriden to <{}>.", prefetchCount);
        }
    }

    private void establishConnection() throws IOException, TimeoutException {
        this.executorService = Executors.newFixedThreadPool(parallelQueues, new ThreadFactoryBuilder()
                .setNameFormat("amqp-consumer-%d " + sourceInput.toIdentifier())
                .setDaemon(true)
                .build());

        final ConnectionFactory factory = new ConnectionFactory();
        factory.setExceptionHandler(new DefaultExceptionHandler() {
            @Override
            public void handleConnectionRecoveryException(Connection conn, Throwable exception) {
                super.handleConnectionRecoveryException(conn, exception);
                inputFailureRecorder.setFailing(AmqpConsumer.class, "Connection recovery error!", exception);
            }
        });
        factory.setTrafficListener(new TrafficListener() {
            @Override
            public void write(Command outboundCommand) {
            }

            @Override
            public void read(Command inboundCommand) {
                inputFailureRecorder.setRunning();
            }
        });

        factory.setHost(hostname);
        factory.setPort(port);
        factory.setVirtualHost(virtualHost);
        factory.setRequestedHeartbeat(heartbeatTimeout);
        // explicitly setting this, to ensure it is true even if the default changes.
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(connectionRecoveryInterval.toMillis());
        factory.setMetricsCollector(metricsCollector);
        factory.setSharedExecutor(executorService);

        if (tls) {
            try {
                LOG.info("Enabling TLS for AMQP input {}.", sourceInput.toIdentifier());
                factory.useSslProtocol();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Couldn't enable TLS for AMQP input.", e);
            }
        }

        // Authenticate?
        if (!isNullOrEmpty(username) && password.isSet()) {
            factory.setUsername(username);
            factory.setPassword(encryptedValueService.decrypt(password));
        }
        connection = factory.newConnection(f("graylog-node-%s-input%s-%s", nodeId.getNodeId(), sourceInput.toIdentifier(), hashCode()));
        connection.addShutdownListener(cause -> {
            if (cause.isInitiatedByApplication()) {
                LOG.info("Shutting down AMPQ consumer.");
                return;
            }

            inputFailureRecorder.setFailing(getClass(),
                    f("AMQP connection lost (reason: %s)! Reconnecting ...", cause.getReason().protocolMethodName()));
        });
        connection.addBlockedListener(new BlockedListener() {
            @Override
            public void handleBlocked(String reason) {
                LOG.warn("AMQP input {} is blocked: {}", sourceInput.toIdentifier(), reason);
            }

            @Override
            public void handleUnblocked() {
                LOG.info("AMQP input {} is not blocked anymore", sourceInput.toIdentifier());
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
        } else if (connection != null) {
            connection.abort();
        }

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public boolean isConnected() {
        return connection != null
                && connection.isOpen()
                && channel != null
                && channel.isOpen();
    }

    public AtomicLong getLastSecBytesRead() {
        return lastSecBytesSupplier.get();
    }

    public AtomicLong getTotalBytesRead() {
        return totalBytesRead;
    }

    public MetricSet getMetricSet() {
        // We are not interested in publish metrics.
        final ImmutableMap<String, Metric> metrics = ImmutableMap.<String, Metric>builder()
                .putAll(metricsCollector.getMetricRegistry().getCounters((name, metric) -> name.matches(".+\\.(?:connections|channels)$")))
                .putAll(metricsCollector.getMetricRegistry().getMeters((name, metric) -> name.matches(".+\\.(?:consumed|acknowledged|rejected)$")))
                .build();
        return MetricSets.of(metrics);
    }
}
