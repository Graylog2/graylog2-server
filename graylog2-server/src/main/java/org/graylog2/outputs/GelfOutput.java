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
package org.graylog2.outputs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
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
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class GelfOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(GelfOutput.class);

    private static final String CK_PROTOCOL = "protocol";
    private static final String CK_HOSTNAME = "hostname";
    private static final String CK_PORT = "port";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final Configuration configuration;
    private final GelfTransport transport;

    @Inject
    public GelfOutput(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this(configuration, buildTransport(configuration));
    }

    @VisibleForTesting
    GelfOutput(Configuration configuration, GelfTransport gelfTransport) {
        this.configuration = checkNotNull(configuration);
        this.transport = checkNotNull(gelfTransport);
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

    protected static GelfTransport buildTransport(final Configuration configuration) throws MessageOutputConfigurationException {
        final String protocol = configuration.getString(CK_PROTOCOL);
        final String hostname = configuration.getString(CK_HOSTNAME);
        final int port = configuration.getInt(CK_PORT);

        if (isNullOrEmpty(protocol) || isNullOrEmpty(hostname) || !configuration.intIsSet(CK_PORT)) {
            throw new MessageOutputConfigurationException("Protocol and/or hostname missing!");
        }

        final GelfConfiguration gelfConfiguration = new GelfConfiguration(hostname, port)
                .transport(GelfTransports.valueOf(protocol.toUpperCase()));

        LOG.debug("Initializing GELF sender and connecting to {}://{}:{}", protocol, hostname, port);

        try {
            return GelfTransports.create(gelfConfiguration);
        } catch (Exception e) {
            final String error = "Error initializing " + GelfOutput.class;
            LOG.error(error, e);
            throw new MessageOutputConfigurationException(error);
        }
    }

    @Override
    public void write(final Message message) throws Exception {
        transport.send(toGELFMessage(message));
    }

    @Override
    public void write(final List<Message> messages) throws Exception {
        for (final Message message : messages) {
            write(message);
        }
    }

    private GelfMessageLevel extractLevel(Object rawLevel) {
        if (rawLevel != null) {
            if (rawLevel instanceof Number) {
                return GelfMessageLevel.fromNumericLevel(((Number) rawLevel).intValue());
            }
        }

        if (rawLevel instanceof String) {
            try {
                return GelfMessageLevel.fromNumericLevel(Integer.getInteger(rawLevel.toString()));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    protected GelfMessage toGELFMessage(final Message message) {
        final DateTime timestamp;
        if (message.getField(Message.FIELD_TIMESTAMP) != null || message.getField(Message.FIELD_TIMESTAMP) instanceof DateTime) {
            timestamp = (DateTime) message.getField(Message.FIELD_TIMESTAMP);
        } else {
            timestamp = Tools.iso8601();
        }

        final GelfMessageLevel messageLevel = extractLevel(message.getField(Message.FIELD_LEVEL));
        final String fullMessage = (String) message.getField(Message.FIELD_FULL_MESSAGE);
        final String facility = (String) message.getField("facility");
        final String forwarder = GelfOutput.class.getCanonicalName();

        final GelfMessageBuilder builder = new GelfMessageBuilder(message.getMessage(), message.getSource())
                .timestamp(timestamp.getMillis() / 1000.0d)
                .additionalField("_forwarder", forwarder)
                .additionalFields(message.getFields());

        if (messageLevel != null) {
            builder.level(messageLevel);
        }

        if (fullMessage != null) {
            builder.fullMessage(fullMessage);
        }

        if (facility != null) {
            builder.additionalField("_facility", facility);
        }

        return builder.build();
    }

    public interface Factory extends MessageOutput.Factory<GelfOutput> {
        @Override
        GelfOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();
            configurationRequest.addField(new TextField(CK_HOSTNAME, "Destination host", "", "This is the hostname of the destination", ConfigurationField.Optional.NOT_OPTIONAL));
            configurationRequest.addField(new NumberField(CK_PORT, "Destination port", 12201, "This is the port of the destination", ConfigurationField.Optional.NOT_OPTIONAL, NumberField.Attribute.IS_PORT_NUMBER));
            final Map<String, String> protocols = ImmutableMap.of(
                    "TCP", "TCP",
                    "UDP", "UDP");
            configurationRequest.addField(new DropdownField(CK_PROTOCOL, "Protocol", "TCP", protocols, "The protocol used to connect", ConfigurationField.Optional.NOT_OPTIONAL));
            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("GELF Output", false, "", "An output sending GELF over TCP or UDP");
        }
    }
}
