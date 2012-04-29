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

import org.graylog2.GraylogServer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.productivity.java.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;

/**
 * SyslogUDPDispatcher.java: 30.04.2012 00:13:02
 *
 * Describe me.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogUDPDispatcher extends FrameDecoder {

    private GraylogServer server;

    public SyslogUDPDispatcher(GraylogServer server) {
        this.server = server;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        byte[] readable = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(readable, buffer.readerIndex(), buffer.readableBytes());

        System.out.println(new String(readable));

        // TODO: only working with structured messages?! this is where to continue tomorrow.
        StructuredSyslogServerEvent e = new StructuredSyslogServerEvent(readable, readable.length, null);
        System.out.println(e.getHost());
        System.out.println(e.getMessage());


        return buffer.readBytes(buffer.readableBytes());
    }

}
