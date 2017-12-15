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
package org.graylog2.plugin.inputs.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionCounterTest {
    private ConnectionCounter connectionCounter;
    private NioEventLoopGroup eventLoopGroup;
    private Channel serverChannel;

    @Before
    public void setUp() throws Exception {
        connectionCounter = new ConnectionCounter(new AtomicInteger(), new AtomicLong());
        eventLoopGroup = new NioEventLoopGroup(1);
        final ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(connectionCounter);

        serverBootstrap.bind(InetAddress.getLocalHost(), 0)
                .addListener((ChannelFutureListener) future -> serverChannel = future.channel())
                .syncUninterruptibly();
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
            clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).syncUninterruptibly();

            // One client active
            assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
            assertThat(connectionCounter.getConnectionCount()).isEqualTo(1);

            clientChannel.close().syncUninterruptibly();
        } finally {
            clientEventLoopGroup.shutdownGracefully();
            clientEventLoopGroup.awaitTermination(1, TimeUnit.SECONDS);
        }

        // No client, but 1 connection so far
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(0);
    }
}