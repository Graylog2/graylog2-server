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
package org.graylog.plugins.beats;

import com.google.inject.assistedinject.Assisted;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.security.encryption.EncryptedValueService;

import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class BeatsTransport extends AbstractTcpTransport {

    private static final String CK_MAX_COMPRESSED_PAYLOAD_SIZE = "max_compressed_payload_size";
    private static final String CK_MAX_DECOMPRESSED_PAYLOAD_SIZE = "max_decompressed_payload_size";
    private final int maxCompressedPayloadSize;
    private final int maxDecompressedPayloadSize;


    @Inject
    public BeatsTransport(@Assisted Configuration configuration,
                          EventLoopGroup eventLoopGroup,
                          EventLoopGroupFactory eventLoopGroupFactory,
                          NettyTransportConfiguration nettyTransportConfiguration,
                          ThroughputCounter throughputCounter,
                          LocalMetricRegistry localRegistry,
                          TLSProtocolsConfiguration tlsConfiguration,
                          EncryptedValueService encryptedValueService) {
        super(configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, tlsConfiguration, encryptedValueService);
        maxCompressedPayloadSize = configuration.getInt(CK_MAX_COMPRESSED_PAYLOAD_SIZE, BeatsFrameDecoder.DEFAULT_MAX_COMPRESSED_PAYLOAD_SIZE);
        maxDecompressedPayloadSize = configuration.getInt(CK_MAX_DECOMPRESSED_PAYLOAD_SIZE, BeatsFrameDecoder.DEFAULT_MAX_DECOMPRESSED_PAYLOAD_SIZE);
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>(super.getCustomChildChannelHandlers(input));
        handlers.put("beats", () -> new BeatsFrameDecoder(maxCompressedPayloadSize, maxDecompressedPayloadSize));

        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<BeatsTransport> {
        @Override
        BeatsTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractTcpTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = super.getRequestedConfiguration();
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(5044);
            }
            cr.addField(new NumberField(
                    CK_MAX_COMPRESSED_PAYLOAD_SIZE,
                    "Maximum allowed compressed payload size",
                    BeatsFrameDecoder.DEFAULT_MAX_COMPRESSED_PAYLOAD_SIZE,
                    "Payloads larger than this value are rejected. If you use a non-default bulk_max_size " +
                            "and have large messages, check that this value is sufficient.",
                    NumberField.Attribute.ONLY_POSITIVE
            ));
            cr.addField(new NumberField(
                    CK_MAX_DECOMPRESSED_PAYLOAD_SIZE,
                    "Maximum allowed decompressed payload size",
                    BeatsFrameDecoder.DEFAULT_MAX_DECOMPRESSED_PAYLOAD_SIZE,
                    "If a payload after decompressing it exceeds this size, it is rejected. This protects against " +
                            "decompression attacks and rarely needs to be adjusted.",
                    NumberField.Attribute.ONLY_POSITIVE
            ));
            return cr;
        }
    }
}
