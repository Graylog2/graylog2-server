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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.rabbitmq.client.ConnectionFactory;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Named;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class AmqpTransport extends ThrottleableTransport2 {
    public static final String CK_HOSTNAME = "broker_hostname";
    public static final String CK_PORT = "broker_port";
    public static final String CK_VHOST = "broker_vhost";
    public static final String CK_USERNAME = "broker_username";
    public static final String CK_PASSWORD = "broker_password";
    public static final String CK_PREFETCH = "prefetch";
    public static final String CK_EXCHANGE = "exchange";
    public static final String CK_EXCHANGE_BIND = "exchange_bind";
    public static final String CK_QUEUE = "queue";
    public static final String CK_ROUTING_KEY = "routing_key";
    public static final String CK_PARALLEL_QUEUES = "parallel_queues";
    public static final String CK_TLS = "tls";
    public static final String CK_REQUEUE_INVALID_MESSAGES = "requeue_invalid_messages";
    public static final String CK_HEARTBEAT_TIMEOUT = "heartbeat";
    public static final String CK_CONNECTION_RECOVERY_INTERVAL = "connection_recovery_interval";

    private static final int DEFAULT_CONNECTION_RECOVERY_INTERVAL_VALUE = 5;
    private static final Logger LOG = LoggerFactory.getLogger(AmqpTransport.class);

    private final Configuration configuration;
    private final EventBus eventBus;
    private final MetricRegistry localRegistry;
    private final EncryptedValueService encryptedValueService;
    private final ScheduledExecutorService scheduler;
    private final ScheduledExecutorService amqpScheduler;

    private AmqpConsumer consumer;

    private InputFailureRecorder inputFailureRecorder;
    private final AtomicReference<ScheduledFuture<?>> scheduledFuture = new AtomicReference<>();

    @AssistedInject
    public AmqpTransport(@Assisted Configuration configuration,
                         EventBus eventBus,
                         LocalMetricRegistry localRegistry,
                         EncryptedValueService encryptedValueService,
                         @Named("daemonScheduler") ScheduledExecutorService scheduler,
                         @Named("AMQP Executor") ScheduledExecutorService amqpScheduler) {
        super(eventBus, configuration);
        this.configuration = configuration;
        this.eventBus = eventBus;
        this.localRegistry = localRegistry;
        this.encryptedValueService = encryptedValueService;
        this.scheduler = scheduler;
        this.amqpScheduler = amqpScheduler;

        localRegistry.register("read_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return consumer.getLastSecBytesRead().get();
            }
        });
        localRegistry.register("written_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return 0L;
            }
        });
        localRegistry.register("read_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return consumer.getTotalBytesRead().get();
            }
        });
        localRegistry.register("written_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return 0L;
            }
        });
    }

    @Subscribe
    public void lifecycleChanged(Lifecycle lifecycle) {
        try {
            LOG.debug("Lifecycle changed to {}", lifecycle);
            switch (lifecycle) {
                case PAUSED, FAILED, HALTING -> stopConsumer();
                default -> {
                    if (consumer.isConnected()) {
                        LOG.debug("Consumer is already connected, not running it a second time.");
                        break;
                    }
                    runConsumer();
                }
            }
        } catch (Exception e) {
            LOG.warn("This should not throw any exceptions", e);
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {

    }

    @Override
    public void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        this.inputFailureRecorder = inputFailureRecorder;

        int heartbeatTimeout = ConnectionFactory.DEFAULT_HEARTBEAT;
        if (configuration.intIsSet(CK_HEARTBEAT_TIMEOUT)) {
            heartbeatTimeout = configuration.getInt(CK_HEARTBEAT_TIMEOUT);
            if (heartbeatTimeout < 0) {
                LOG.warn("AMQP heartbeat interval must not be negative ({}), using default timeout ({}).",
                        heartbeatTimeout, ConnectionFactory.DEFAULT_HEARTBEAT);
                heartbeatTimeout = ConnectionFactory.DEFAULT_HEARTBEAT;
            }
        }

        consumer = new AmqpConsumer(
                heartbeatTimeout,
                input,
                configuration,
                scheduler,
                inputFailureRecorder,
                this,
                encryptedValueService,
                connectionRecoveryInterval()
        );
        eventBus.register(this);
        runConsumer();
    }

    private void runConsumer() {
        cancelConsumerRunScheduler();
        scheduledFuture.set(scheduleConsumerRun());
    }

    private ScheduledFuture<?> scheduleConsumerRun() {
        return amqpScheduler.scheduleAtFixedRate(() -> {
            try {
                consumer.run();
                cancelConsumerRunScheduler();
                inputFailureRecorder.setRunning();
            } catch (TimeoutException e) {
                inputFailureRecorder.setFailing(getClass(), "Timeout while opening new AMQP connection", e);
            } catch (Exception e) {
                inputFailureRecorder.setFailing(getClass(), "Could not launch AMQP consumer.", e);
            }
        }, 0, connectionRecoveryInterval().getSeconds(), TimeUnit.SECONDS);
    }

    private void cancelConsumerRunScheduler() {
        scheduledFuture.updateAndGet(f -> {
            if (f != null) {
                f.cancel(true);
            }
            return null;
        });
    }

    private Duration connectionRecoveryInterval() {
        return Duration.ofSeconds(configuration.getInt(CK_CONNECTION_RECOVERY_INTERVAL, DEFAULT_CONNECTION_RECOVERY_INTERVAL_VALUE));
    }

    @Override
    public void doStop() {
        stopConsumer();
        eventBus.unregister(this);
    }

    private void stopConsumer() {
        try {
            if (consumer != null) {
                consumer.stop();
            }
        } catch (IOException e) {
            LOG.warn("Unable to stop consumer", e);
        }

        cancelConsumerRunScheduler();
    }

    @Override
    public com.codahale.metrics.MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<AmqpTransport> {
        @Override
        AmqpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport2.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = super.getRequestedConfiguration();
            cr.addField(
                    new TextField(
                            CK_HOSTNAME,
                            "Broker hostname",
                            "",
                            "Hostname of the AMQP broker to use",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            cr.addField(
                    new NumberField(
                            CK_PORT,
                            "Broker port",
                            5672,
                            "Port of the AMQP broker to use",
                            ConfigurationField.Optional.OPTIONAL,
                            NumberField.Attribute.IS_PORT_NUMBER
                    )
            );

            cr.addField(
                    new TextField(
                            CK_VHOST,
                            "Broker virtual host",
                            "/",
                            "Virtual host of the AMQP broker to use",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            cr.addField(
                    new TextField(
                            CK_USERNAME,
                            "Username",
                            "",
                            "Username to connect to AMQP broker",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );

            cr.addField(
                    new TextField(
                            CK_PASSWORD,
                            "Password",
                            "",
                            "Password to connect to AMQP broker",
                            ConfigurationField.Optional.OPTIONAL,
                            true,
                            TextField.Attribute.IS_PASSWORD
                    )
            );

            cr.addField(
                    new NumberField(
                            CK_PREFETCH,
                            "Prefetch count",
                            100,
                            "For advanced usage: AMQP prefetch count. Default is 100.",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            cr.addField(
                    new TextField(
                            CK_QUEUE,
                            "Queue",
                            defaultQueueName(),
                            "Name of queue that is created.",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            cr.addField(
                    new TextField(
                            CK_EXCHANGE,
                            "Exchange",
                            defaultExchangeName(),
                            "Name of exchange to bind to.",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            cr.addField(
                    new BooleanField(
                            CK_EXCHANGE_BIND,
                            "Bind to exchange",
                            false,
                            "Binds the queue to the configured exchange. The exchange must already exist."
                    )
            );

            cr.addField(
                    new TextField(
                            CK_ROUTING_KEY,
                            "Routing key",
                            defaultRoutingKey(),
                            "Routing key to listen for.",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            cr.addField(
                    new NumberField(
                            CK_PARALLEL_QUEUES,
                            "Number of Queues",
                            1,
                            "Number of parallel Queues",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            cr.addField(
                    new NumberField(
                            CK_HEARTBEAT_TIMEOUT,
                            "Heartbeat timeout",
                            ConnectionFactory.DEFAULT_HEARTBEAT,
                            "Heartbeat interval in seconds (use 0 to disable heartbeat)",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );

            cr.addField(
                    new BooleanField(
                            CK_TLS,
                            "Enable TLS?",
                            false,
                            "Enable transport encryption via TLS. (requires valid TLS port setting)"
                    )
            );

            cr.addField(
                    new BooleanField(
                            CK_REQUEUE_INVALID_MESSAGES,
                            "Re-queue invalid messages?",
                            true,
                            "Invalid messages will be discarded if disabled."
                    )
            );
            cr.addField(
                    new NumberField(
                            CK_CONNECTION_RECOVERY_INTERVAL,
                            "Connection recovery interval",
                            DEFAULT_CONNECTION_RECOVERY_INTERVAL_VALUE,
                            "Connection recovery interval in seconds.",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );

            return cr;
        }

        protected String defaultRoutingKey() {
            return "#";
        }

        protected String defaultExchangeName() {
            return "log-messages";
        }

        protected String defaultQueueName() {
            return "log-messages";
        }
    }
}
