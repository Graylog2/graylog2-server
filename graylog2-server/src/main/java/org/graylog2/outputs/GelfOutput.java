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
import org.graylog2.plugin.configuration.fields.BooleanField;
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

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class GelfOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(GelfOutput.class);

    private static final String CK_PROTOCOL = "protocol";
    private static final String CK_HOSTNAME = "hostname";
    private static final String CK_PORT = "port";
    private static final String CK_CONNECT_TIMEOUT = "connect_timeout";
    private static final String CK_RECONNECT_DELAY = "reconnect_delay";
    private static final String CK_TCP_NO_DELAY = "tcp_no_delay";
    private static final String CK_TCP_KEEP_ALIVE = "tcp_keep_alive";
    private static final String CK_TLS_VERIFICATION_ENABLED = "tls_verification_enabled";
    private static final String CK_TLS_TRUST_CERT_CHAIN = "tls_trust_cert_chain";
    private static final String CK_QUEUE_SIZE = "queue_size";
    private static final String CK_MAX_INFLIGHT_SENDS = "max_inflight_sends";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final GelfTransport transport;

    @Inject
    public GelfOutput(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this(buildTransport(configuration));
    }

    @VisibleForTesting
    GelfOutput(GelfTransport gelfTransport) {
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
        final int connectTimeout = configuration.getInt(CK_CONNECT_TIMEOUT, 1000);
        final int reconnectDelay = configuration.getInt(CK_RECONNECT_DELAY, 500);
        final boolean tcpKeepAlive = configuration.getBoolean(CK_TCP_KEEP_ALIVE, false);
        final boolean tcpNoDelay = configuration.getBoolean(CK_TCP_NO_DELAY, false);
        final boolean tlsVerificationEnabled = configuration.getBoolean(CK_TLS_VERIFICATION_ENABLED, false);
        final String tlsTrustCertChain = configuration.getString(CK_TLS_TRUST_CERT_CHAIN);
        final int queueSize = configuration.getInt(CK_QUEUE_SIZE, 512);
        final int maxInflightSends = configuration.getInt(CK_MAX_INFLIGHT_SENDS, 512);

        if (isNullOrEmpty(protocol) || isNullOrEmpty(hostname) || !configuration.intIsSet(CK_PORT)) {
            throw new MessageOutputConfigurationException("Protocol and/or hostname missing!");
        }

        final GelfTransports transport;
        final boolean tlsEnabled;
        switch (protocol.toUpperCase(Locale.ENGLISH)) {
            case "UDP":
                transport = GelfTransports.UDP;
                tlsEnabled = false;
                break;
            case "TCP":
                transport = GelfTransports.TCP;
                tlsEnabled = false;
                break;
            case "TCP+TLS":
                transport = GelfTransports.TCP;
                tlsEnabled = true;
                break;
            default:
                throw new MessageOutputConfigurationException("Unknown protocol " + protocol);
        }

        final File tlsTrustCertChainFile;
        if (tlsEnabled && !isNullOrEmpty(tlsTrustCertChain)) {
            tlsTrustCertChainFile = new File(tlsTrustCertChain);

            if (!tlsTrustCertChainFile.isFile() && !tlsTrustCertChainFile.canRead()) {
                throw new MessageOutputConfigurationException("TLS trust certificate chain file cannot be read!");
            }
        } else {
            tlsTrustCertChainFile = null;
        }

        final GelfConfiguration gelfConfiguration = new GelfConfiguration(hostname, port)
                .transport(transport)
                .connectTimeout(connectTimeout)
                .reconnectDelay(reconnectDelay)
                .tcpKeepAlive(tcpKeepAlive)
                .tcpNoDelay(tcpNoDelay)
                .queueSize(queueSize)
                .maxInflightSends(maxInflightSends);

        if (tlsEnabled) {
            gelfConfiguration.enableTls();

            if (tlsVerificationEnabled) {
                gelfConfiguration.enableTlsCertVerification();
            } else {
                gelfConfiguration.disableTlsCertVerification();
            }

            if (tlsTrustCertChainFile != null) {
                gelfConfiguration.tlsTrustCertChainFile(tlsTrustCertChainFile);
            }
        }

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

    @Nullable
    private GelfMessageLevel extractLevel(Object rawLevel) {
        GelfMessageLevel level;
        if (rawLevel instanceof Number) {
            final int numericLevel = ((Number) rawLevel).intValue();
            level = extractLevel(numericLevel);
        } else if (rawLevel instanceof String) {
            Integer numericLevel;
            try {
                numericLevel = Integer.parseInt((String) rawLevel);
            } catch (NumberFormatException e) {
                LOG.debug("Invalid message level " + rawLevel, e);
                numericLevel = null;
            }

            if (numericLevel == null) {
                level = null;
            } else {
                level = extractLevel(numericLevel);
            }
        } else {
            LOG.debug("Invalid message level {}", rawLevel);
            level = null;
        }

        return level;
    }

    @Nullable
    private GelfMessageLevel extractLevel(int numericLevel) {
        GelfMessageLevel level;
        try {
            level = GelfMessageLevel.fromNumericLevel(numericLevel);
        } catch (IllegalArgumentException e) {
            LOG.debug("Invalid numeric message level " + numericLevel, e);
            level = null;
        }
        return level;
    }

    protected GelfMessage toGELFMessage(final Message message) {
        final DateTime timestamp;
        final Object fieldTimeStamp = message.getField(Message.FIELD_TIMESTAMP);
        if (fieldTimeStamp instanceof DateTime) {
            timestamp = (DateTime) fieldTimeStamp;
        } else {
            timestamp = Tools.nowUTC();
        }

        final GelfMessageLevel messageLevel = extractLevel(message.getField(Message.FIELD_LEVEL));
        final String fullMessage = (String) message.getField(Message.FIELD_FULL_MESSAGE);
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
            final Map<String, String> protocols = ImmutableMap.of(
                    "TCP", "TCP",
                    "TCP+TLS", "TCP+TLS",
                    "UDP", "UDP");
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();
            configurationRequest.addField(new TextField(CK_HOSTNAME, "Destination host", "", "This is the hostname of the destination", ConfigurationField.Optional.NOT_OPTIONAL));
            configurationRequest.addField(new NumberField(CK_PORT, "Destination port", 12201, "This is the port of the destination", ConfigurationField.Optional.NOT_OPTIONAL, NumberField.Attribute.IS_PORT_NUMBER));
            configurationRequest.addField(new DropdownField(CK_PROTOCOL, "Protocol", "TCP", protocols, "The protocol used to connect", ConfigurationField.Optional.NOT_OPTIONAL));
            configurationRequest.addField(new NumberField(CK_CONNECT_TIMEOUT, "TCP Connect Timeout", 1000, "Connection timeout for TCP connections in milliseconds", ConfigurationField.Optional.OPTIONAL, NumberField.Attribute.ONLY_POSITIVE));
            configurationRequest.addField(new NumberField(CK_RECONNECT_DELAY, "TCP Reconnect Delay", 500, "Time to wait between reconnects in milliseconds", ConfigurationField.Optional.OPTIONAL, NumberField.Attribute.ONLY_POSITIVE));
            configurationRequest.addField(new BooleanField(CK_TCP_NO_DELAY, "TCP No Delay", false, "Whether to use Nagle's algorithm for TCP connections"));
            configurationRequest.addField(new BooleanField(CK_TCP_KEEP_ALIVE, "TCP Keep Alive", false, "Whether to send TCP keep alive packets"));
            configurationRequest.addField(new BooleanField(CK_TLS_VERIFICATION_ENABLED, "TLS verification", false, "Whether to verify peers when using TLS"));
            configurationRequest.addField(new TextField(CK_TLS_TRUST_CERT_CHAIN, "TLS Trust Certificate Chain", "", "Local file which contains the trust certificate chain", ConfigurationField.Optional.OPTIONAL));
            configurationRequest.addField(new NumberField(CK_QUEUE_SIZE, "Internal buffer size", 512, "Buffer size to support asynchronous writes", ConfigurationField.Optional.OPTIONAL, NumberField.Attribute.ONLY_POSITIVE));
            configurationRequest.addField(new NumberField(CK_MAX_INFLIGHT_SENDS, "Concurrent network requests", 512, "Maximum number of concurrent network operations until spinning", ConfigurationField.Optional.OPTIONAL, NumberField.Attribute.ONLY_POSITIVE));

            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("GELF Output", false, "", "An output sending GELF over TCP or UDP");
        }
    }
}
