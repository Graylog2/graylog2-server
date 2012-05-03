/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.inputs.syslog;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * SyslogUDPDispatcher.java: 30.04.2012 00:13:02
 *
 * Describe me.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogUDPDispatcher extends SimpleChannelHandler {

    private static final Logger LOG = Logger.getLogger(SyslogUDPDispatcher.class);

    private SyslogProcessor processor;

    public SyslogUDPDispatcher(GraylogServer server) {
        this.processor = new SyslogProcessor(server);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) e.getRemoteAddress();

        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();

        byte[] readable = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(readable, buffer.readerIndex(), buffer.readableBytes());

        this.processor.messageReceived(new String(readable), remoteAddress.getAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.warn("Could not handle syslog message.", e.getCause());
    }

}
