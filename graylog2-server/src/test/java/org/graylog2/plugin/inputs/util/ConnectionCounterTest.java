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
package org.graylog2.plugin.inputs.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionCounterTest {
    private static final int LATCH_TIMEOUT = 30;

    private ConnectionCounter connectionCounter;
    private NioEventLoopGroup eventLoopGroup;
    private Channel serverChannel;
    private CountDownLatch readCompleteLatch;
    private CountDownLatch disconnectedLatch;

    @Before
    public void setUp() throws Exception {
        readCompleteLatch = new CountDownLatch(1);
        disconnectedLatch = new CountDownLatch(1);
        connectionCounter = new ConnectionCounter(new AtomicInteger(), new AtomicLong());
        eventLoopGroup = new NioEventLoopGroup(1);
        final ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addFirst("connection-counter", connectionCounter);

                        // This adds some latches to allow us to wait until the server processed our connection.
                        // Without this, this test was failing from time to time because we checked the connection
                        // counter values before the connection was actually processed.
                        ch.pipeline().addLast("latches", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
                                disconnectedLatch.countDown();
                                super.channelUnregistered(ctx);
                            }

                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                readCompleteLatch.countDown();
                                super.channelReadComplete(ctx);
                            }
                        });
                    }
                });

        final CountDownLatch serverChannelLatch = new CountDownLatch(1);
        serverBootstrap.bind(InetAddress.getLocalHost(), 0)
                .addListener((ChannelFutureListener) future -> {
                    serverChannel = future.channel();
                    serverChannelLatch.countDown();
                })
                .syncUninterruptibly();
        // Wait for serverChannel to be set to make sure we can use it in tests
        serverChannelLatch.await(LATCH_TIMEOUT, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        serverChannel.close().syncUninterruptibly();
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void testConnectAndDisconnect() throws Exception {
        // Fresh channel, no connections so far
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(0L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(0);

        final EventLoopGroup clientEventLoopGroup = new NioEventLoopGroup(1);
        try {
            final ChannelFuture connectFuture = new Bootstrap()
                    .group(clientEventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler())
                    .localAddress(InetAddress.getLocalHost(), 0)
                    .connect(serverChannel.localAddress())
                    .sync();
            final Channel clientChannel = connectFuture.channel();

            assertThat(clientChannel.isWritable()).isTrue();
            // We have to send a message here to make sure that channelReadComplete gets called in the handler
            clientChannel.writeAndFlush(Unpooled.wrappedBuffer("canary".getBytes(StandardCharsets.UTF_8))).syncUninterruptibly();

            // Wait until the server read the message
            readCompleteLatch.await(LATCH_TIMEOUT, TimeUnit.SECONDS);

            // One client active
            assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
            assertThat(connectionCounter.getConnectionCount()).isEqualTo(1);

            clientChannel.close().syncUninterruptibly();
        } finally {
            clientEventLoopGroup.shutdownGracefully();
            clientEventLoopGroup.awaitTermination(1, TimeUnit.SECONDS);
        }

        // Wait until the client has disconnected
        disconnectedLatch.await(LATCH_TIMEOUT, TimeUnit.SECONDS);

        // No client, but 1 connection so far
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(0);
    }
}