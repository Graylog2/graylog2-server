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

import com.github.joschi.jadconfig.util.Size;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.graylog2.inputs.transports.netty.DatagramChannelFactory;
import org.graylog2.inputs.transports.netty.DatagramPacketHandler;
import org.graylog2.inputs.transports.netty.EnvelopeMessageAggregationHandler;
import org.graylog2.inputs.transports.netty.EnvelopeMessageHandler;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.inputs.transports.netty.NettyTransportType;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class UdpTransport extends NettyTransport {
    private static final Logger LOG = LoggerFactory.getLogger(UdpTransport.class);

    private final NettyTransportConfiguration nettyTransportConfiguration;
    private final ChannelGroup channels;
    private EventLoopGroup eventLoopGroup;
    private Bootstrap bootstrap;

    @AssistedInject
    public UdpTransport(@Assisted Configuration configuration,
                        EventLoopGroupFactory eventLoopGroupFactory,
                        NettyTransportConfiguration nettyTransportConfiguration,
                        ThroughputCounter throughputCounter,
                        LocalMetricRegistry localRegistry) {
        super(configuration, eventLoopGroupFactory, throughputCounter, localRegistry);
        this.nettyTransportConfiguration = nettyTransportConfiguration;
        this.channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    @VisibleForTesting
    Bootstrap getBootstrap(MessageInput input) {
        LOG.debug("Setting UDP receive buffer size to {} bytes", getRecvBufferSize());
        final NettyTransportType transportType = nettyTransportConfiguration.getType();

        eventLoopGroup = eventLoopGroupFactory.create(workerThreads, localRegistry, "workers");

        return new Bootstrap()
                .group(eventLoopGroup)
                .channelFactory(new DatagramChannelFactory(transportType))
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(getRecvBufferSize()))
                .option(ChannelOption.SO_RCVBUF, getRecvBufferSize())
                .option(UnixChannelOption.SO_REUSEPORT, true)
                .handler(getChannelInitializer(getChannelHandlers(input)))
                .validate();
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>(super.getChannelHandlers(input));
        handlers.put("traffic-counter", () -> throughputCounter);
        handlers.put("udp-datagram", () -> DatagramPacketHandler.INSTANCE);
        handlers.putAll(getChildChannelHandlers(input));

        return handlers;
    }

    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getChildChannelHandlers(final MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlerList = new LinkedHashMap<>(getCustomChildChannelHandlers(input));

        final CodecAggregator aggregator = getAggregator();
        if (aggregator != null) {
            LOG.debug("Adding codec aggregator {} to channel pipeline", aggregator);
            handlerList.put("codec-aggregator", () -> new EnvelopeMessageAggregationHandler(aggregator, localRegistry));
        }
        handlerList.put("envelope-message-handler", () -> new EnvelopeMessageHandler(input));

        return handlerList;
    }

    @Override
    public void launch(final MessageInput input) throws MisfireException {
        try {
            bootstrap = getBootstrap(input);

            final NettyTransportType transportType = nettyTransportConfiguration.getType();
            int numChannels = (transportType == NettyTransportType.EPOLL || transportType == NettyTransportType.KQUEUE) ? workerThreads : 1;
            for (int i = 0; i < numChannels; i++) {
                LOG.debug("Starting channel on {}", socketAddress);
                bootstrap.bind(socketAddress)
                        .addListener(new InputLaunchListener(channels, input, getRecvBufferSize()))
                        .syncUninterruptibly();
            }
        } catch (Exception e) {
            throw new MisfireException(e);
        }
    }


    @Override
    public void stop() {
        if (channels != null) {
            channels.close().syncUninterruptibly();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        bootstrap = null;
    }

    @Nullable
    @Override
    public SocketAddress getLocalAddress() {
        if (channels != null) {
            return channels.stream().findFirst().map(Channel::localAddress).orElse(null);
        }

        return null;
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<UdpTransport> {
        @Override
        UdpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends NettyTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            final int recvBufferSize = Ints.saturatedCast(Size.kilobytes(256L).toBytes());
            r.addField(ConfigurationRequest.Templates.recvBufferSize(CK_RECV_BUFFER_SIZE, recvBufferSize));

            return r;
        }
    }

    private static class InputLaunchListener implements ChannelFutureListener {
        private final ChannelGroup channels;
        private final MessageInput input;
        private final int expectedRecvBufferSize;

        public InputLaunchListener(ChannelGroup channels, MessageInput input, int expectedRecvBufferSize) {
            this.channels = channels;
            this.input = input;
            this.expectedRecvBufferSize = expectedRecvBufferSize;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                final Channel channel = future.channel();
                channels.add(channel);
                LOG.debug("Started channel {}", channel);

                final DatagramChannelConfig channelConfig = (DatagramChannelConfig) channel.config();
                final int receiveBufferSize = channelConfig.getReceiveBufferSize();
                if (receiveBufferSize != expectedRecvBufferSize) {
                    LOG.warn("receiveBufferSize (SO_RCVBUF) for input {} (channel {}) should be {} but is {}.",
                            input, channel, expectedRecvBufferSize, receiveBufferSize);
                }
            } else {
                LOG.warn("Failed to start channel for input {}", input, future.cause());
            }
        }
    }
}
