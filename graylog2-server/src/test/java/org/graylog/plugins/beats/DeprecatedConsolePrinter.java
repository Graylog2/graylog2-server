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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Ints;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import static com.google.common.base.MoreObjects.firstNonNull;

public class DeprecatedConsolePrinter {
    public static void main(String[] args) throws Exception {
        String hostname = "127.0.0.1";
        int port = 5044;
        if (args.length >= 2) {
            hostname = args[0];
            port = firstNonNull(Ints.tryParse(args[1]), 5044);
        }
        if (args.length >= 1) {
            port = firstNonNull(Ints.tryParse(args[1]), 5044);
        }


        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            final ServerBootstrap b = new ServerBootstrap()
                    .group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("logging", new LoggingHandler());
                            ch.pipeline().addLast("beats-frame-decoder", new BeatsFrameDecoder());
                            ch.pipeline().addLast("beats-deprecated-codec", new BeatsCodecHandler());
                        }
                    });

            System.out.println("Starting listener on " + hostname + ":" + port);
            final ChannelFuture future = b.bind(hostname, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public static class BeatsCodecHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        private final BeatsCodec beatsCodec = new BeatsCodec(Configuration.EMPTY_CONFIGURATION, objectMapper);

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf message) throws Exception {
            final int readableBytes = message.readableBytes();
            final byte[] messageBytes = new byte[readableBytes];
            message.readBytes(messageBytes);
            final RawMessage rawMessage = new RawMessage(messageBytes);

            final Message decodedMessage = beatsCodec.decode(rawMessage);
            System.out.println(decodedMessage);

            ctx.fireChannelRead(decodedMessage);
        }
    }
}
