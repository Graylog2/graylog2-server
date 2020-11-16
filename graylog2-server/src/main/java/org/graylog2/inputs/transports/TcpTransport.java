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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.inputs.transports.netty.LenientDelimiterBasedFrameDecoder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.Delimiters.lineDelimiter;
import static io.netty.handler.codec.Delimiters.nulDelimiter;

public class TcpTransport extends AbstractTcpTransport {
    public static final String CK_USE_NULL_DELIMITER = "use_null_delimiter";
    private static final String CK_MAX_MESSAGE_SIZE = "max_message_size";
    private static final int DEFAULT_MAX_FRAME_LENGTH = 2 * 1024 * 1024;

    protected final ByteBuf[] delimiter;
    protected final int maxFrameLength;

    @AssistedInject
    public TcpTransport(@Assisted Configuration configuration,
                        EventLoopGroup eventLoopGroup,
                        EventLoopGroupFactory eventLoopGroupFactory,
                        NettyTransportConfiguration nettyTransportConfiguration,
                        ThroughputCounter throughputCounter,
                        LocalMetricRegistry localRegistry,
                        org.graylog2.Configuration graylogConfiguration) {
        super(configuration, throughputCounter, localRegistry, eventLoopGroup, eventLoopGroupFactory, nettyTransportConfiguration, graylogConfiguration);

        final boolean nulDelimiter = configuration.getBoolean(CK_USE_NULL_DELIMITER);
        this.delimiter = nulDelimiter ? nulDelimiter() : lineDelimiter();
        this.maxFrameLength = configuration.getInt(CK_MAX_MESSAGE_SIZE, DEFAULT_MAX_FRAME_LENGTH);
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> childChannelHandlers = new LinkedHashMap<>();

        childChannelHandlers.put("framer", () -> new LenientDelimiterBasedFrameDecoder(maxFrameLength, delimiter));
        childChannelHandlers.putAll(super.getCustomChildChannelHandlers(input));

        return childChannelHandlers;
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<TcpTransport> {
        @Override
        TcpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractTcpTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest x = super.getRequestedConfiguration();

            x.addField(
                    new BooleanField(
                            CK_USE_NULL_DELIMITER,
                            "Null frame delimiter?",
                            false,
                            "Use null byte as frame delimiter? Otherwise newline delimiter is used."
                    )
            );
            x.addField(
                    new NumberField(
                            CK_MAX_MESSAGE_SIZE,
                            "Maximum message size",
                            DEFAULT_MAX_FRAME_LENGTH,
                            "The maximum length of a message.",
                            ConfigurationField.Optional.OPTIONAL,
                            NumberField.Attribute.ONLY_POSITIVE
                    )
            );

            return x;
        }
    }
}
