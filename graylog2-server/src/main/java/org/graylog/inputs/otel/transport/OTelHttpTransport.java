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
package org.graylog.inputs.otel.transport;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import jakarta.inject.Named;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.AbstractHttpTransport;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.utilities.IpSubnet;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.Callable;

public class OTelHttpTransport extends AbstractHttpTransport {
    public static final String NAME = "otel-http";

    // 4 MB default matches the OpenTelemetry Collector's default max_recv_msg_size_mib for gRPC.
    // The inherited 64 KB default is too small for typical OTLP log batches.
    static final int DEFAULT_OTLP_MAX_CHUNK_SIZE = 4 * 1024 * 1024;

    private final OTelJournalRecordFactory journalRecordFactory;

    @AssistedInject
    public OTelHttpTransport(@Assisted Configuration configuration,
                             EventLoopGroup eventLoopGroup,
                             EventLoopGroupFactory eventLoopGroupFactory,
                             NettyTransportConfiguration nettyTransportConfiguration,
                             ThroughputCounter throughputCounter,
                             LocalMetricRegistry localMetricRegistry,
                             TLSProtocolsConfiguration tlsConfiguration,
                             @Named("trusted_proxies") Set<IpSubnet> trustedProxies,
                             OTelJournalRecordFactory journalRecordFactory) {
        super(configuration, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration,
                throughputCounter, localMetricRegistry, tlsConfiguration, trustedProxies, OTelHttpHandler.LOGS_PATH);
        this.journalRecordFactory = journalRecordFactory;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers =
                new LinkedHashMap<>(super.getCustomChildChannelHandlers(input));
        handlers.replace("http-handler", () -> new OTelHttpHandler(journalRecordFactory, input));
        handlers.remove("http-bulk-newline-decoder");
        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<OTelHttpTransport> {
        @Override
        OTelHttpTransport create(Configuration configuration);

        @Override
        OTelHttpTransport.Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractHttpTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            r.addField(ConfigurationRequest.Templates.portNumber(CK_PORT, 4318));
            r.addField(new NumberField("max_chunk_size",
                    "Max. HTTP chunk size",
                    DEFAULT_OTLP_MAX_CHUNK_SIZE,
                    "The maximum HTTP chunk size in bytes (e. g. length of HTTP request body)",
                    ConfigurationField.Optional.OPTIONAL));
            return r;
        }
    }
}
