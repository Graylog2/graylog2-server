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
package org.graylog.plugins.sidecar.opamp.server;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OpAMPService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAMPService.class);
    private static final int DEFAULT_PORT = 4320;

    private final boolean enabled;
    private final HostAndPort bindAddress;
    private final int bossThreads;
    private final int workerThreads;
    private final OpAMPChannelInitializer channelInitializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Inject
    public OpAMPService(@Named("opamp_enabled") boolean enabled,
                        @Named("opamp_bind_address") String bindAddress,
                        @Named("opamp_boss_threads") int bossThreads,
                        @Named("opamp_worker_threads") int workerThreads,
                        OpAMPChannelInitializer channelInitializer) {
        this.enabled = enabled;
        this.bindAddress = HostAndPort.fromString(bindAddress).withDefaultPort(DEFAULT_PORT);
        this.bossThreads = bossThreads;
        this.workerThreads = workerThreads;
        this.channelInitializer = channelInitializer;
    }

    @Override
    protected void startUp() throws Exception {
        if (!enabled) {
            LOG.info("OpAMP server is disabled");
            return;
        }

        LOG.info("Starting OpAMP WebSocket server on {}", bindAddress);

        bossGroup = new NioEventLoopGroup(bossThreads,
                new ThreadFactoryBuilder()
                        .setNameFormat("opamp-boss-%d")
                        .setDaemon(true)
                        .build());

        workerGroup = new NioEventLoopGroup(workerThreads,
                new ThreadFactoryBuilder()
                        .setNameFormat("opamp-worker-%d")
                        .setDaemon(true)
                        .build());

        final ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(channelInitializer);

        serverChannel = bootstrap.bind(bindAddress.getHost(), bindAddress.getPort())
                .sync()
                .channel();

        LOG.info("OpAMP WebSocket server started on {}", bindAddress);
    }

    @Override
    protected void shutDown() throws Exception {
        if (!enabled) {
            return;
        }

        LOG.info("Stopping OpAMP WebSocket server");

        if (serverChannel != null) {
            serverChannel.close().sync();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        LOG.info("OpAMP WebSocket server stopped");
    }
}
