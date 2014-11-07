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
package org.graylog2.outputs;

import com.google.common.collect.ImmutableMap;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GelfOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(GelfOutput.class);

    private static final String CK_PROTOCOL = "protocol";
    private static final String CK_HOSTNAME = "hostname";
    private static final String CK_PORT = "port";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private Configuration configuration;
    private GelfTransport transport;

    @Override
    public void initialize(final Configuration config) throws MessageOutputConfigurationException {
        configuration = config;
        transport = buildTransport(configuration);
        isRunning.set(true);
    }

    @Override
    public void stop() {
        LOG.debug("Stopping {}", transport.getClass().getName());
        try {
            transport.stop();
        } catch (Exception e) {
            LOG.error("Error stopping " + transport.getClass().getName(), e);
        }
        isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    protected GelfTransport buildTransport(final Configuration configuration) throws MessageOutputConfigurationException {
        final String protocol = configuration.getString(CK_PROTOCOL).toUpperCase();
        final String hostname = configuration.getString(CK_HOSTNAME);
        final int port = configuration.getInt(CK_PORT);

        final GelfConfiguration gelfConfiguration = new GelfConfiguration(new InetSocketAddress(hostname, port))
                .transport(GelfTransports.valueOf(protocol));

        LOG.debug("Initializing GELF sender and connecting to {}://{}:{}", protocol, hostname, port);

        final GelfTransport gelfTransport;
        try {
            gelfTransport = GelfTransports.create(gelfConfiguration);
        } catch (Exception e) {
            final String error = "Error initializing " + this.getClass() + ": " + e.getMessage();
            LOG.error(error, e);
            throw new MessageOutputConfigurationException(error);
        }

        return gelfTransport;
    }

    @Override
    public void write(final Message message) throws Exception {
        if (transport == null) {
            transport = buildTransport(this.configuration);
        }
        transport.send(toGELFMessage(message));
    }

    @Override
    public void write(final List<Message> messages) throws Exception {
        for (final Message message : messages) {
            write(message);
        }
    }

    protected GelfMessage toGELFMessage(final Message message) {
        final DateTime timestamp;
        if (message.getField("timestamp") != null || message.getField("timestamp") instanceof DateTime) {
            timestamp = (DateTime) message.getField("timestamp");
        } else {
            timestamp = Tools.iso8601();
        }

        final Integer level = (Integer) message.getField("level");
        final GelfMessageLevel messageLevel = level == null ? GelfMessageLevel.ALERT : GelfMessageLevel.fromNumericLevel(level);
        final String fullMessage = (String) message.getField("message");
        final String facility = (String) message.getField("facility");
        final String forwarder = GelfOutput.class.getCanonicalName();

        final GelfMessageBuilder builder = new GelfMessageBuilder(message.getMessage(), message.getSource())
                .timestamp(timestamp.getMillis())
                .level(messageLevel)
                .additionalField("_forwarder", forwarder)
                .additionalFields(message.getFields());

        if (fullMessage != null) {
            builder.fullMessage(fullMessage);
        }

        if (facility != null) {
            builder.additionalField("_facility", facility);
        }

        return builder.build();
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.addField(new TextField(CK_HOSTNAME, "Destination host", "", "This is the hostname of the destination", ConfigurationField.Optional.NOT_OPTIONAL));
        configurationRequest.addField(new NumberField(CK_PORT, "Destination port", 12201, "This is the port of the destination", ConfigurationField.Optional.NOT_OPTIONAL));
        final Map<String, String> protocols = ImmutableMap.of(
                "TCP", "TCP",
                "UDP", "UDP");
        configurationRequest.addField(new DropdownField(CK_PROTOCOL, "Protocol", "TCP", protocols, "The protocol used to connect", ConfigurationField.Optional.OPTIONAL));
        return configurationRequest;
    }

    @Override
    public String getName() {
        return "GELF Output";
    }

    @Override
    public String getHumanName() {
        return "An output sending GELF over TCP or UDP";
    }

    @Override
    public String getLinkToDocs() {
        return null;
    }
}
