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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AMQPInput extends MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(AMQPInput.class);

    public static final String NAME = "AMQP Input";

    private Consumer consumer;

    public static final String CK_HOSTNAME = "broker_hostname";
    public static final String CK_PORT = "broker_port";
    public static final String CK_VHOST = "broker_vhost";
    public static final String CK_USERNAME = "broker_username";
    public static final String CK_PASSWORD = "broker_password";
    public static final String CK_PREFETCH = "prefetch";
    public static final String CK_EXCHANGE = "exchange";
    public static final String CK_QUEUE = "queue";
    public static final String CK_ROUTING_KEY = "routing_key";

    @Override
    public void launch() throws MisfireException {
        setupMetrics();

        consumer = new Consumer(
                configuration.getString(CK_HOSTNAME),
                (int) configuration.getInt(CK_PORT),
                configuration.getString(CK_VHOST),
                configuration.getString(CK_USERNAME),
                configuration.getString(CK_PASSWORD),
                (int) configuration.getInt(CK_PREFETCH),
                configuration.getString(CK_QUEUE),
                configuration.getString(CK_EXCHANGE),
                configuration.getString(CK_ROUTING_KEY),
                graylogServer,
                this
        );

        try {
            consumer.run();
        } catch(IOException e) {
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
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest cr = new ConfigurationRequest();

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
                        0,
                        "For advanced usage: AMQP prefetch count. Default is 0 (unlimited).",
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
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        if (!checkConfig(configuration)) {
            throw new ConfigurationException(configuration.getSource().toString());
        }
    }

    protected boolean checkConfig(Configuration configuration) {
        return configuration.stringIsSet(CK_HOSTNAME)
                && configuration.intIsSet(CK_PORT)
                && configuration.stringIsSet(CK_QUEUE)
                && configuration.stringIsSet(CK_EXCHANGE)
                && configuration.stringIsSet(CK_ROUTING_KEY);
    }

    private void setupMetrics() {
        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "read_bytes_1sec"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return consumer.getLastSecBytesRead().get();
            }
        });

        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "written_bytes_1sec"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return 0L;
            }
        });

        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "read_bytes_total"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return consumer.getTotalBytesRead().get();
            }
        });

        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "written_bytes_total"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return 0L;
            }
        });

    }

}
