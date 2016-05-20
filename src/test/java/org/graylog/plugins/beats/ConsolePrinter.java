/**
 * This file is part of Graylog Beats Plugin.
 *
 * Graylog Beats Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Beats Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Beats Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Ints;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class ConsolePrinter {
    public static void main(String[] args) {
        String hostname = "127.0.0.1";
        int port = 5044;
        if (args.length >= 2) {
            hostname = args[0];
            port = firstNonNull(Ints.tryParse(args[1]), 5044);
        }
        if (args.length >= 1) {
            port = firstNonNull(Ints.tryParse(args[1]), 5044);
        }

        final ChannelFactory factory =
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool());
        final ServerBootstrap b = new ServerBootstrap(factory);
        b.getPipeline().addLast("beats-frame-decoder", new BeatsFrameDecoder());
        b.getPipeline().addLast("beats-codec", new BeatsCodecHandler());
        b.getPipeline().addLast("logging", new LoggingHandler());
        System.out.println("Starting listener on " + hostname + ":" + port);
        b.bind(new InetSocketAddress(hostname, port));
    }

    public static class BeatsCodecHandler extends SimpleChannelUpstreamHandler {
        private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        private final BeatsCodec beatsCodec = new BeatsCodec(Configuration.EMPTY_CONFIGURATION, objectMapper);

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            final ChannelBuffer message = (ChannelBuffer) e.getMessage();
            final int readableBytes = message.readableBytes();
            final byte[] messageBytes = new byte[readableBytes];
            message.readBytes(messageBytes);
            final RawMessage rawMessage = new RawMessage(messageBytes);

            final Message decodedMessage = beatsCodec.decode(rawMessage);
            System.out.println(decodedMessage);

            super.messageReceived(ctx, e);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            e.getCause().printStackTrace();

            final Channel ch = e.getChannel();
            ch.close();
        }
    }
}
