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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.TransportFactory;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

public class AmqpTransport extends ThrottleableTransport {
    public static final String CK_HOSTNAME = "broker_hostname";
    public static final String CK_PORT = "broker_port";
    public static final String CK_VHOST = "broker_vhost";
    public static final String CK_USERNAME = "broker_username";
    public static final String CK_PASSWORD = "broker_password";
    public static final String CK_PREFETCH = "prefetch";
    public static final String CK_EXCHANGE = "exchange";
    public static final String CK_QUEUE = "queue";
    public static final String CK_ROUTING_KEY = "routing_key";

    private static final Logger LOG = LoggerFactory.getLogger(AmqpTransport.class);

    private final Configuration configuration;
    private final EventBus eventBus;
    private final MetricRegistry metricRegistry;
    private final ScheduledExecutorService scheduler;

    private AmqpConsumer consumer;

    @AssistedInject
    public AmqpTransport(@Assisted Configuration configuration,
                         EventBus eventBus,
                         MetricRegistry metricRegistry,
                         @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.configuration = configuration;
        this.eventBus = eventBus;
        this.metricRegistry = metricRegistry;
        this.scheduler = scheduler;
    }

    @Subscribe
    public void lifecycleChanged(Lifecycle lifecycle) {
        try {
            LOG.debug("Lifecycle changed to {}", lifecycle);
            switch (lifecycle) {
                case RUNNING:
                    if (consumer.isConnected()) {
                        LOG.debug("Consumer is already connected, not running it a second time.");
                        break;
                    }
                    try {
                        consumer.run();
                    } catch (IOException e) {
                        LOG.warn("Unable to resume consumer", e);
                    }
                    break;
                default:
                    try {
                        if (consumer != null) {
                            consumer.stop();
                        }
                    } catch (IOException e) {
                        LOG.warn("Unable to stop consumer", e);
                    }
                    break;
            }
        } catch (Exception e) {
            LOG.warn("This should not throw any exceptions", e);
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {

    }

    @Override
    public void launch(MessageInput2 input) throws MisfireException {
        consumer = new AmqpConsumer(
                configuration.getString(CK_HOSTNAME),
                (int) configuration.getInt(CK_PORT),
                configuration.getString(CK_VHOST),
                configuration.getString(CK_USERNAME),
                configuration.getString(CK_PASSWORD),
                (int) configuration.getInt(CK_PREFETCH),
                configuration.getString(CK_QUEUE),
                configuration.getString(CK_EXCHANGE),
                configuration.getString(CK_ROUTING_KEY),
                input,
                scheduler
        );
        eventBus.register(this);
        try {
            consumer.run();
        } catch (IOException e) {
            throw new MisfireException("Could not launch AMQP consumer.", e);
        }
    }

    @Override
    public void stop() {
        if (consumer != null) {
            try {
                consumer.stop();
            } catch (IOException e) {
                LOG.error("Could not stop AMQP consumer.", e);
            }
        }
        eventBus.unregister(this);
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest cr = new ConfigurationRequest();

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
                        "log-messages",
                        "Name of queue that is created.",
                        ConfigurationField.Optional.NOT_OPTIONAL
                )
        );

        cr.addField(
                new TextField(
                        CK_EXCHANGE,
                        "Exchange",
                        "log-messages",
                        "Name of exchange to bind to.",
                        ConfigurationField.Optional.NOT_OPTIONAL
                )
        );

        cr.addField(
                new TextField(
                        CK_ROUTING_KEY,
                        "Routing key",
                        "#",
                        "Routing key to listen for.",
                        ConfigurationField.Optional.NOT_OPTIONAL
                )
        );

        return cr;
    }

    @Override
    public void setupMetrics(MessageInput2 input) {
        metricRegistry.register(MetricRegistry.name(input.getUniqueReadableId(), "read_bytes_1sec"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return consumer.getLastSecBytesRead().get();
            }
        });

        metricRegistry.register(MetricRegistry.name(input.getUniqueReadableId(), "written_bytes_1sec"),
                                new Gauge<Long>() {
                                    @Override
                                    public Long getValue() {
                                        return 0L;
                                    }
                                });

        metricRegistry.register(MetricRegistry.name(input.getUniqueReadableId(), "read_bytes_total"),
                                new Gauge<Long>() {
                                    @Override
                                    public Long getValue() {
                                        return consumer.getTotalBytesRead().get();
                                    }
                                });

        metricRegistry.register(MetricRegistry.name(input.getUniqueReadableId(), "written_bytes_total"),
                                new Gauge<Long>() {
                                    @Override
                                    public Long getValue() {
                                        return 0L;
                                    }
                                });

    }

    public interface Factory extends TransportFactory<AmqpTransport> {
        @Override
        AmqpTransport create(Configuration configuration);
    }
}
