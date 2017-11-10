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

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionCounterTest {
    private ConnectionCounter connectionCounter;
    private NioEventLoopGroup eventLoopGroup;
    private ChannelFuture channelFuture;
    private Channel serverChannel;

    @Before
    public void setUp() throws Exception {
        connectionCounter = new ConnectionCounter();
        eventLoopGroup = new NioEventLoopGroup(1);
        final ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(connectionCounter);

        channelFuture = serverBootstrap.bind(InetAddress.getLocalHost(), 0)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        assertThat(future).isDone();
                        serverChannel = future.channel();
                    }
                })
                .sync();
    }

    @After
    public void tearDown() throws Exception {
        serverChannel.close().sync();
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    public void testConnectAndDisconnect() throws Exception {
        // Fresh channel, no connections so far
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(0L);
        assertThat(connectionCounter.gaugeTotal().getValue()).isEqualTo(0L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(0);
        assertThat(connectionCounter.gaugeCurrent().getValue()).isEqualTo(0);

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
            clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).sync();

            // One client active
            assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
            assertThat(connectionCounter.gaugeTotal().getValue()).isEqualTo(1L);
            assertThat(connectionCounter.getConnectionCount()).isEqualTo(1);
            assertThat(connectionCounter.gaugeCurrent().getValue()).isEqualTo(1);

            clientChannel.close();
            clientChannel.closeFuture().sync();
        } finally {
            clientEventLoopGroup.shutdownGracefully();
        }

        // Give the server socket time to realize the client is gone
        // TODO: How to avoid this arbitrary sleep?
        Thread.sleep(100L);

        // No client, but 1 connection so far
        assertThat(connectionCounter.getTotalConnections()).isEqualTo(1L);
        assertThat(connectionCounter.gaugeTotal().getValue()).isEqualTo(1L);
        assertThat(connectionCounter.getConnectionCount()).isEqualTo(0);
        assertThat(connectionCounter.gaugeCurrent().getValue()).isEqualTo(0);
    }
}